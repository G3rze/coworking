package com.gerson.coworking.service;

import com.gerson.coworking.domain.event.ReservationEvent;
import com.gerson.coworking.domain.enums.ReservationStatus;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsReservationEventListener {

    private final Counter reservationsConfirmedCounter;
    private final Counter reservationsCancelledCounter;
    private final AtomicLong activeReservationsGaugeValue;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationEvent(ReservationEvent event) {
        if (event.getNewStatus() == ReservationStatus.CONFIRMED
                && event.getPreviousStatus() == ReservationStatus.PENDING_PAYMENT) {
            reservationsConfirmedCounter.increment();
            activeReservationsGaugeValue.incrementAndGet();
            log.debug("Metrics updated: reservation {} confirmed", event.getReservation().getId());
        }

        if (event.getNewStatus() == ReservationStatus.CANCELLED) {
            reservationsCancelledCounter.increment();
            if (event.getPreviousStatus() == ReservationStatus.CONFIRMED) {
                activeReservationsGaugeValue.decrementAndGet();
            }
            log.debug("Metrics updated: reservation {} cancelled", event.getReservation().getId());
        }
    }
}
