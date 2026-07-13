package com.gerson.coworking.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response with JWT token")
public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Token type", example = "Bearer")
        String type,

        @Schema(description = "Token expiration time in milliseconds", example = "86400000")
        Long expiresIn,

        @Schema(description = "Authenticated user information")
        UserInfo user
) {
    public LoginResponse {
        if (type == null) {
            type = "Bearer";
        }
    }
}
