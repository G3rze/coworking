package com.gerson.coworking.domain.strategy;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

@Component
@Primary
public class PeakHoursPricingStrategy implements PricingStrategy {

    private static final LocalTime PEAK_START_1 = LocalTime.of(9, 0);
    private static final LocalTime PEAK_END_1 = LocalTime.of(12, 0);
    private static final LocalTime PEAK_START_2 = LocalTime.of(14, 0);
    private static final LocalTime PEAK_END_2 = LocalTime.of(18, 0);
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.5");

    @Override
    public BigDecimal calculatePrice(BigDecimal pricePerHour, LocalTime startTime, LocalTime endTime) {
        BigDecimal normalHours = getNormalHours(startTime, endTime);
        BigDecimal peakHours = getPeakHours(startTime, endTime);

        BigDecimal normalPrice = pricePerHour.multiply(normalHours);
        BigDecimal peakPrice = pricePerHour.multiply(peakHours).multiply(PEAK_MULTIPLIER);

        return normalPrice.add(peakPrice);
    }

    private BigDecimal getPeakHours(LocalTime start, LocalTime end) {
        long peakHours1 = calculatePeakHoursInRange(
                maxOf(start, PEAK_START_1),
                minOf(end, PEAK_END_1));

        long peakHours2 = calculatePeakHoursInRange(
                maxOf(start, PEAK_START_2),
                minOf(end, PEAK_END_2));

        return BigDecimal.valueOf(Math.max(0, peakHours1 + peakHours2));
    }

    private long calculatePeakHoursInRange(LocalTime rangeStart, LocalTime rangeEnd) {
        if (rangeStart.isBefore(rangeEnd)) {
            return Duration.between(rangeStart, rangeEnd).toHours();
        }
        return 0;
    }

    private BigDecimal getNormalHours(LocalTime start, LocalTime end) {
        long totalMinutes = Duration.between(start, end).toMinutes();
        if (totalMinutes <= 0) {
            totalMinutes = 60;
        }

        long totalHours = totalMinutes / 60;
        if (totalMinutes % 60 > 0) {
            totalHours++;
        }

        return BigDecimal.valueOf(totalHours).subtract(getPeakHours(start, end));
    }

    private LocalTime maxOf(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalTime minOf(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
