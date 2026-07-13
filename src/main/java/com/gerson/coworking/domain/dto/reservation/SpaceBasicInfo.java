package com.gerson.coworking.domain.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Basic space information embedded in reservation response")
public record SpaceBasicInfo(
        @Schema(description = "Space unique identifier")
        UUID id,

        @Schema(description = "Space name")
        String name,

        @Schema(description = "Space location")
        String location
) {}
