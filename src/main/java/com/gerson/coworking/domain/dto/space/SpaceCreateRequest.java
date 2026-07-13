package com.gerson.coworking.domain.dto.space;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Space creation request")
public record SpaceCreateRequest(
        @Schema(description = "Space name (max 100 characters)", example = "Conference Room A")
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Schema(description = "Space description (max 500 characters)", example = "Large meeting room with projector")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Schema(description = "Maximum capacity (minimum 1)", example = "10")
        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @Schema(description = "Physical location of the space", example = "Floor 2, Building A")
        @NotBlank(message = "Location is required")
        @Size(max = 200, message = "Location must be at most 200 characters")
        String location,

        @Schema(description = "Price per hour in USD", example = "25.00")
        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.01", message = "Price per hour must be greater than 0")
        BigDecimal pricePerHour
) {}
