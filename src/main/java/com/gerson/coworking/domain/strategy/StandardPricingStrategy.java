package com.gerson.coworking.domain.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

@Component
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(BigDecimal pricePerHour, LocalTime startTime, LocalTime endTime) {
        long hours = Duration.between(startTime, endTime).toHours();
        if (hours <= 0) {
            hours = 1;
        }
        return pricePerHour.multiply(BigDecimal.valueOf(hours));
    }
}
