package com.gerson.coworking.domain.strategy;

import java.util.UUID;

public interface PaymentStrategy {

    PaymentResult processPayment(UUID reservationId, java.math.BigDecimal amount);

    record PaymentResult(boolean success, String message, String reference) {}
}
