package com.gerson.coworking.domain.state;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.enums.ReservationStatus;

public interface ReservationState {

    ReservationStatus getStatus();

    boolean canConfirm();

    boolean canCancel();

    Reservation confirm(Reservation reservation);

    Reservation cancel(Reservation reservation);
}
