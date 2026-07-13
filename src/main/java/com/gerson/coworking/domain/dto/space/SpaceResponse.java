package com.gerson.coworking.domain.dto.space;

import com.gerson.coworking.domain.enums.SpaceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Schema(description = "Space information response")
public record SpaceResponse(
        @Schema(description = "Space unique identifier")
        UUID id,

        @Schema(description = "Space name")
        String name,

        @Schema(description = "Space description")
        String description,

        @Schema(description = "Maximum capacity")
        Integer capacity,

        @Schema(description = "Physical location")
        String location,

        @Schema(description = "Price per hour in USD")
        BigDecimal pricePerHour,

        @Schema(description = "Current availability status")
        SpaceStatus status,

        @Schema(description = "Creation timestamp in America/El_Salvador timezone")
        ZonedDateTime createdAt
) {}
