package com.gerson.coworking.domain.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record SpaceOccupancyDto(
    UUID spaceId,
    String spaceName,
    long reservedHours,
    long availableHours,
    BigDecimal occupancyPercentage
) {}
