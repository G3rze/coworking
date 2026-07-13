package com.gerson.coworking.domain.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Basic user information embedded in reservation response")
public record UserBasicInfo(
        @Schema(description = "User unique identifier")
        UUID id,

        @Schema(description = "Username")
        String username,

        @Schema(description = "Email address")
        String email
) {}
