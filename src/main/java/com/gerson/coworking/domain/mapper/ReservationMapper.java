package com.gerson.coworking.domain.mapper;

import com.gerson.coworking.domain.dto.reservation.ReservationResponse;
import com.gerson.coworking.domain.dto.reservation.SpaceBasicInfo;
import com.gerson.coworking.domain.dto.reservation.UserBasicInfo;
import com.gerson.coworking.domain.entity.Reservation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class ReservationMapper {

    private ReservationMapper() {}

    public static ReservationResponse toResponse(Reservation reservation, ZoneId zone) {
        SpaceBasicInfo spaceBasicInfo = new SpaceBasicInfo(
                reservation.getSpace().getId(),
                reservation.getSpace().getName(),
                reservation.getSpace().getLocation()
        );

        UserBasicInfo userBasicInfo = new UserBasicInfo(
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getUser().getEmail()
        );

        ZonedDateTime createdAtZoned = convertToZonedDateTime(reservation.getCreatedAt(), zone);

        return new ReservationResponse(
                reservation.getId(),
                spaceBasicInfo,
                userBasicInfo,
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getTotalPrice(),
                reservation.getPaymentReference(),
                createdAtZoned
        );
    }

    private static ZonedDateTime convertToZonedDateTime(Instant instant, ZoneId zone) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneOffset.UTC).withZoneSameInstant(zone);
    }
}
