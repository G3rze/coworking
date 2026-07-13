package com.gerson.coworking.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request payload")
public record LoginRequest(
        @Schema(description = "Username of the user", example = "admin")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(description = "Password of the user", example = "admin123")
        @NotBlank(message = "Password is required")
        String password
) {}
