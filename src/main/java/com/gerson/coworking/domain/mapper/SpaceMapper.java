package com.gerson.coworking.domain.mapper;

import com.gerson.coworking.domain.dto.space.SpaceResponse;
import com.gerson.coworking.domain.entity.Space;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class SpaceMapper {

    private SpaceMapper() {}

    public static SpaceResponse toResponse(Space space, ZoneId zone) {
        ZonedDateTime createdAtZoned = convertToZonedDateTime(space.getCreatedAt(), zone);

        return new SpaceResponse(
                space.getId(),
                space.getName(),
                space.getDescription(),
                space.getCapacity(),
                space.getLocation(),
                space.getPricePerHour(),
                space.getStatus(),
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
