package com.gerson.coworking.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Basic user information returned after authentication")
public record UserInfo(
        @Schema(description = "User unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        java.util.UUID id,

        @Schema(description = "Username", example = "admin")
        String username,

        @Schema(description = "User role", example = "ADMIN")
        com.gerson.coworking.domain.enums.Role role
) {}
