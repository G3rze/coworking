package com.gerson.coworking.domain.dto.auth;

import com.gerson.coworking.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Basic user information returned after authentication")
public record UserInfo(
        @Schema(description = "User unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Username", example = "admin")
        String username,

        @Schema(description = "User role", example = "ADMIN")
        Role role
) {}
