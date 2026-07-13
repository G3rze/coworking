package com.gerson.coworking.domain.state;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.enums.ReservationStatus;

public interface ReservationState {

    ReservationStatus getStatus();

    boolean canConfirm();

    boolean canCancel();

    boolean canComplete();

    Reservation confirm(Reservation reservation);

    Reservation cancel(Reservation reservation);

    Reservation complete(Reservation reservation);
}
