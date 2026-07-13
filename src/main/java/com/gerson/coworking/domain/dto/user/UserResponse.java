package com.gerson.coworking.domain.dto.user;

import com.gerson.coworking.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;
import java.util.UUID;

@Schema(description = "User information response")
public record UserResponse(
        @Schema(description = "User unique identifier")
        UUID id,

        @Schema(description = "Username")
        String username,

        @Schema(description = "Email address")
        String email,

        @Schema(description = "User role (ADMIN or USER)")
        Role role,

        @Schema(description = "Account creation timestamp in America/El_Salvador timezone")
        ZonedDateTime createdAt
) {}
