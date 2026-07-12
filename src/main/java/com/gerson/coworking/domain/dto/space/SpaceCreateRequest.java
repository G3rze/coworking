package com.gerson.coworking.domain.dto.space;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SpaceCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @NotBlank(message = "Location is required")
        @Size(max = 200, message = "Location must be at most 200 characters")
        String location,

        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.01", message = "Price per hour must be greater than 0")
        BigDecimal pricePerHour
) {}
