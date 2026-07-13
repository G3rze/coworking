package com.gerson.coworking.domain.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Reservation creation request")
public record ReservationCreateRequest(
        @Schema(description = "Space unique identifier to reserve", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "Space ID is required")
        UUID spaceId,

        @Schema(description = "Reservation date (must be today or future)", example = "2026-07-15")
        @NotNull(message = "Date is required")
        @FutureOrPresent(message = "Date must be today or in the future")
        LocalDate date,

        @Schema(description = "Start time of reservation", example = "09:00")
        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @Schema(description = "End time of reservation (must be after start time)", example = "12:00")
        @NotNull(message = "End time is required")
        LocalTime endTime
) {}
