package com.gerson.coworking.domain.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OccupancyReportResponse(
    LocalDate dateFrom,
    LocalDate dateTo,
    long totalDays,
    List<SpaceOccupancyDto> spaces,
    BigDecimal averageOccupancy
) {}
