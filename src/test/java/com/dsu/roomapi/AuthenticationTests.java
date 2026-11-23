package com.dsu.roomapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationTests {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtEncoder jwtEncoder;
    @MockitoBean AppUserRepository users;

    @Test
    void missingLoginHasTheSameErrorOnProtectedRoutes() throws Exception {
        for (String path : new String[]{"/api/auth/me", "/api/reservations", "/api/reservations/1"}) {
            mvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("login required"))
                    .andExpect(jsonPath("$.path").value(path))
                    .andExpect(jsonPath("$.fieldErrors").isEmpty());
        }
        mvc.perform(post("/api/rooms").contentType("application/json")
                        .content("{\"name\":\"Lab\",\"capacity\":4}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("login required"));
    }

    @Test
    void expiredAndTamperedTokensFailEvenOnPublicRoomReads() throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("room-api").subject("mina")
                .issuedAt(now.minusSeconds(600)).expiresAt(now.minusSeconds(120)).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        for (String token : new String[]{expired, "broken.token.signature"}) {
            mvc.perform(get("/api/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("token is invalid or expired"))
                    .andExpect(jsonPath("$.fieldErrors").isEmpty());
        }
    }

    @Test
    void validJwtAuthenticatesWithoutLoadingTheAccount() throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("room-api").subject("lee")
                .issuedAt(now).expiresAt(now.plusSeconds(1800)).claim("role", "USER").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("lee"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"));
        verifyNoInteractions(users);
    }

    @Test
    void userTokenCannotCreateRoomsAndReceivesApiError() throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("room-api").subject("mina")
                .issuedAt(now).expiresAt(now.plusSeconds(1800)).claim("role", "USER").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(post("/api/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType("application/json").content("{\"name\":\"Lab\",\"capacity\":4}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("admin role required"))
                .andExpect(jsonPath("$.path").value("/api/rooms"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void adminTokenContainsTheRoleAndExposesAdminAuthority() throws Exception {
        when(users.findByUsername("admin")).thenReturn(
                Optional.of(new AppUser("admin", encoder.encode("admin1234"), "ADMIN")));
        String response = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"admin1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = new tools.jackson.databind.ObjectMapper().readTree(response).get("accessToken").asText();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_ADMIN"));
    }

    @Test
    void signupReturnsOnlyPublicFieldsAndPersistsABcryptHash() throws Exception {
        when(users.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/api/auth/signup").contentType("application/json")
                        .content("{\"username\":\"lee\",\"password\":\"lee12345\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("lee"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        var captured = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(users).saveAndFlush(captured.capture());
        assertTrue(encoder.matches("lee12345", captured.getValue().getPasswordHash()));
        assertNotEquals("lee12345", captured.getValue().getPasswordHash());
    }

    @Test
    void bcryptLimitUsesUtf8Bytes() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType("application/json")
                        .content("{\"username\":\"lee\",\"password\":\"" + "я".repeat(37) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password must be at most 72 UTF-8 bytes"));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void unknownUserAndWrongPasswordHaveTheSameResponse() throws Exception {
        when(users.findByUsername("mina")).thenReturn(
                Optional.of(new AppUser("mina", encoder.encode("mina1234"), "USER")));
        for (String username : new String[]{"mina", "missing"}) {
            mvc.perform(post("/api/auth/login").contentType("application/json")
                            .content("{\"username\":\"" + username + "\",\"password\":\"wrongpassword\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("wrong username or password"))
                    .andExpect(jsonPath("$.fieldErrors").isEmpty());
        }
    }
}
