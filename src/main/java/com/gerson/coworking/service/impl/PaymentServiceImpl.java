package com.gerson.coworking.service.impl;

import com.gerson.coworking.domain.strategy.PaymentStrategy;
import com.gerson.coworking.service.PaymentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentStrategy paymentStrategy;

    public PaymentServiceImpl(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    @Override
    public PaymentValidationResult validatePayment(UUID reservationId, BigDecimal amount) {
        PaymentStrategy.PaymentResult result = paymentStrategy.processPayment(reservationId, amount);
        return new PaymentValidationResult(result.success(), result.message());
    }
}
