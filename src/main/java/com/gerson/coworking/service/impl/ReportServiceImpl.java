package com.gerson.coworking.service.impl;

import com.gerson.coworking.domain.dto.report.OccupancyReportResponse;
import com.gerson.coworking.domain.dto.report.SpaceOccupancyDto;
import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.repository.ReservationRepository;
import com.gerson.coworking.repository.SpaceRepository;
import com.gerson.coworking.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@CacheConfig(cacheNames = "occupancy")
public class ReportServiceImpl implements ReportService {

    private static final int BUSINESS_HOURS_PER_DAY = 12;

    private final SpaceRepository spaceRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Cacheable(key = "#dateFrom.toString() + '-' + #dateTo.toString()")
    public OccupancyReportResponse getOccupancyReport(LocalDate dateFrom, LocalDate dateTo) {
        long totalDays = ChronoUnit.DAYS.between(dateFrom, dateTo) + 1;
        long availableHoursPerSpace = totalDays * BUSINESS_HOURS_PER_DAY;

        List<Reservation> confirmedReservations = reservationRepository
                .findConfirmedReservationsInDateRange(dateFrom, dateTo);

        Map<UUID, Double> reservedHoursMap = new HashMap<>();
        for (Reservation reservation : confirmedReservations) {
            UUID spaceId = reservation.getSpace().getId();
            long hours = Duration.between(reservation.getStartTime(), reservation.getEndTime()).toHours();
            if (hours <= 0) {
                hours = 1;
            }
            reservedHoursMap.merge(spaceId, (double) hours, Double::sum);
        }

        List<SpaceOccupancyDto> spaceOccupancies = new ArrayList<>();
        BigDecimal totalOccupancy = BigDecimal.ZERO;

        for (Space space : spaceRepository.findAll()) {
            double reservedHours = reservedHoursMap.getOrDefault(space.getId(), 0.0);
            BigDecimal occupancy = BigDecimal.valueOf(reservedHours)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(availableHoursPerSpace), 2, RoundingMode.HALF_UP);

            spaceOccupancies.add(new SpaceOccupancyDto(
                    space.getId(),
                    space.getName(),
                    (long) reservedHours,
                    availableHoursPerSpace,
                    occupancy
            ));

            totalOccupancy = totalOccupancy.add(occupancy);
        }

        BigDecimal averageOccupancy = spaceOccupancies.isEmpty()
                ? BigDecimal.ZERO
                : totalOccupancy.divide(BigDecimal.valueOf(spaceOccupancies.size()), 2, RoundingMode.HALF_UP);

        return new OccupancyReportResponse(dateFrom, dateTo, totalDays, spaceOccupancies, averageOccupancy);
    }
}
