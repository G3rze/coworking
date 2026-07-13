package com.gerson.coworking.domain.strategy;

import java.math.BigDecimal;
import java.time.LocalTime;

public interface PricingStrategy {

    BigDecimal calculatePrice(BigDecimal pricePerHour, LocalTime startTime, LocalTime endTime);
}
