package com.gerson.coworking.domain.mapper;

import com.gerson.coworking.domain.dto.user.UserResponse;
import com.gerson.coworking.domain.entity.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user, ZoneId zone) {
        ZonedDateTime createdAtZoned = convertToZonedDateTime(user.getCreatedAt(), zone);

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
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
