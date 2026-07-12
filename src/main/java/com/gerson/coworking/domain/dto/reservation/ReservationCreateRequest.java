package com.gerson.coworking.domain.dto.reservation;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationCreateRequest(
        @NotNull(message = "Space ID is required")
        UUID spaceId,

        @NotNull(message = "Date is required")
        @FutureOrPresent(message = "Date must be today or in the future")
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime
) {}
