package com.gerson.coworking.domain.dto.reservation;

import com.gerson.coworking.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationFilterRequest(
        UUID spaceId,
        LocalDate dateFrom,
        LocalDate dateTo,
        ReservationStatus status
) {}
