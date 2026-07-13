package com.gerson.coworking.domain.dto.reservation;

import com.gerson.coworking.domain.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Schema(description = "Reservation information response")
public record ReservationResponse(
        @Schema(description = "Reservation unique identifier")
        UUID id,

        @Schema(description = "Reserved space information")
        SpaceBasicInfo space,

        @Schema(description = "User who made the reservation")
        UserBasicInfo user,

        @Schema(description = "Reservation date")
        LocalDate date,

        @Schema(description = "Start time")
        LocalTime startTime,

        @Schema(description = "End time")
        LocalTime endTime,

        @Schema(description = "Current reservation status")
        ReservationStatus status,

        @Schema(description = "Total price calculated for this reservation")
        BigDecimal totalPrice,

        @Schema(description = "Payment reference code (if payment has been processed)")
        String paymentReference,

        @Schema(description = "Reservation creation timestamp in America/El_Salvador timezone")
        ZonedDateTime createdAt
) {}
