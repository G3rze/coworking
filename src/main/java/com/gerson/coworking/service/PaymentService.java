package com.gerson.coworking.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {

    PaymentValidationResult validatePayment(UUID reservationId, BigDecimal amount);

    record PaymentValidationResult(boolean success, String message) {}
}
