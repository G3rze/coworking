package com.gerson.coworking.service.impl;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    @Async("notificationExecutor")
    public void sendReservationConfirmation(Reservation reservation) {
        log.info("=== SIMULATED EMAIL ===");
        log.info("To: {}", reservation.getUser().getEmail());
        log.info("Subject: Reserva confirmada - {}", reservation.getSpace().getName());
        log.info("Body: Su reserva del {} de {} a {} ha sido confirmada.",
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime());
        log.info("========================");
    }

    @Override
    @Async("notificationExecutor")
    public void sendReservationCancellation(Reservation reservation) {
        log.info("=== SIMULATED EMAIL ===");
        log.info("To: {}", reservation.getUser().getEmail());
        log.info("Subject: Reserva cancelada - {}", reservation.getSpace().getName());
        log.info("Body: Su reserva del {} de {} a {} ha sido cancelada.",
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime());
        log.info("========================");
    }
}
