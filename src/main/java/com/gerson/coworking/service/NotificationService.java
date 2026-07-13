package com.gerson.coworking.service;

import com.gerson.coworking.domain.entity.Reservation;

public interface NotificationService {
    void sendReservationConfirmation(Reservation reservation);
    void sendReservationCancellation(Reservation reservation);
}
