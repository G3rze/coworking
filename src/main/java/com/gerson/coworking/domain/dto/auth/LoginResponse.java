package com.gerson.coworking.domain.dto.auth;

public record LoginResponse(
        String token,
        String type,
        Long expiresIn,
        UserInfo user
) {
    public LoginResponse {
        if (type == null) {
            type = "Bearer";
        }
    }
}
