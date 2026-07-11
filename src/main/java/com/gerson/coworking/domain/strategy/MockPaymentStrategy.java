package com.gerson.coworking.domain.strategy;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Component
@Primary
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallback")
public class MockPaymentStrategy implements PaymentStrategy {

    private final Random random = new Random();

    @Override
    public PaymentResult processPayment(UUID reservationId, BigDecimal amount) {
        try {
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = random.nextDouble() < 0.9;

        if (success) {
            String reference = "PAY-" + reservationId.toString().substring(0, 8).toUpperCase();
            return new PaymentResult(true, "Payment validated successfully", reference);
        } else {
            return new PaymentResult(false, "Payment validation failed", null);
        }
    }

    public PaymentResult fallback(UUID reservationId, BigDecimal amount, Throwable t) {
        return new PaymentResult(
                true,
                "Payment service unavailable - reservation remains PENDING",
                null
        );
    }
}
