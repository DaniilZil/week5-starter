package com.dsu.roomapi;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "password is required") String password) {
    @Override
    public String toString() {
        return "LoginRequest[credentials redacted]";
    }
}
