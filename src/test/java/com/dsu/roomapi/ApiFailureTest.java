package com.dsu.roomapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiFailureTest {
    private static final String ROOM_JSON = """
            {"name":"Test Room","capacity":5}
            """;
    private static final String RESERVATIONS = "/api/rooms/1/reservations";

    @Autowired
    private MockMvc mvc;

    private static String basic(String username) {
        String credentials = username + ":" + username + "1234";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void noLoginIs401() throws Exception {
        mvc.perform(delete("/api/rooms/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("login required"));
    }

    @Test
    void userCreatingRoomIs403() throws Exception {
        mvc.perform(post("/api/rooms").header(AUTHORIZATION, basic("mina"))
                        .contentType(APPLICATION_JSON).content(ROOM_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("admin role required"));
    }

    @Test
    void adminCreatingRoomIs201() throws Exception {
        mvc.perform(post("/api/rooms").header(AUTHORIZATION, basic("admin"))
                        .contentType(APPLICATION_JSON).content(ROOM_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().exists(LOCATION))
                .andExpect(jsonPath("$.name").value("Test Room"))
                .andExpect(jsonPath("$.capacity").value(5));
    }

    @Test
    void overlapIs409() throws Exception {
        mvc.perform(post(RESERVATIONS).header(AUTHORIZATION, basic("omar"))
                        .contentType(APPLICATION_JSON).content("""
                                {"startTime":"2026-10-06T10:30:00","endTime":"2026-10-06T11:30:00"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("room 1 is already booked at that time"));
    }

    @Test
    void endBeforeStartIs400() throws Exception {
        mvc.perform(post(RESERVATIONS).header(AUTHORIZATION, basic("mina"))
                        .contentType(APPLICATION_JSON).content("""
                                {"startTime":"2026-10-06T12:00:00","endTime":"2026-10-06T11:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startTime must be before endTime"));
    }

    @Test
    void anotherUsersReservationIs403() throws Exception {
        mvc.perform(get("/api/reservations/1").header(AUTHORIZATION, basic("omar")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("reservation 1 belongs to another user"));
    }

    @Test
    void deleteRoomWithReservationsIs409() throws Exception {
        mvc.perform(delete("/api/rooms/1").header(AUTHORIZATION, basic("admin")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("room 1 has reservations"));
    }

    @Test
    void missingFieldIs400() throws Exception {
        mvc.perform(post(RESERVATIONS).header(AUTHORIZATION, basic("mina"))
                        .contentType(APPLICATION_JSON).content("""
                                {"startTime":"2026-10-06T12:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("request has invalid fields"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("endTime"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("endTime is required"));
    }
}
