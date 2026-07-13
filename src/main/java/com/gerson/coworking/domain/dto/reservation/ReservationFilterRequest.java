package com.gerson.coworking.domain.dto.reservation;

import com.gerson.coworking.domain.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Reservation filter criteria for searching")
public record ReservationFilterRequest(
        @Schema(description = "Filter by space ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID spaceId,

        @Schema(description = "Filter reservations from this date (inclusive)", example = "2026-07-01")
        LocalDate dateFrom,

        @Schema(description = "Filter reservations until this date (inclusive)", example = "2026-07-31")
        LocalDate dateTo,

        @Schema(description = "Filter by reservation status", example = "CONFIRMED")
        ReservationStatus status
) {}
