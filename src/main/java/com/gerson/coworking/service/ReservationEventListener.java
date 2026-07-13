package com.gerson.coworking.service;

import com.gerson.coworking.domain.event.ReservationEvent;
import com.gerson.coworking.domain.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationEvent(ReservationEvent event) {
        if (event.getNewStatus() == ReservationStatus.CONFIRMED) {
            log.debug("Reservation confirmed, sending notification: {}", event.getReservation().getId());
            notificationService.sendReservationConfirmation(event.getReservation());
        } else if (event.getNewStatus() == ReservationStatus.CANCELLED) {
            log.debug("Reservation cancelled, sending notification: {}", event.getReservation().getId());
            notificationService.sendReservationCancellation(event.getReservation());
        }
    }
}
