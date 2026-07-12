package com.gerson.coworking.domain.dto.space;

import com.gerson.coworking.domain.enums.SpaceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SpaceResponse(
        UUID id,
        String name,
        String description,
        Integer capacity,
        String location,
        BigDecimal pricePerHour,
        SpaceStatus status,
        Instant createdAt
) {}
