package com.gerson.coworking.domain.dto.reservation;

import com.gerson.coworking.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        SpaceBasicInfo space,
        UserBasicInfo user,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        ReservationStatus status,
        BigDecimal totalPrice,
        String paymentReference,
        Instant createdAt
) {}
