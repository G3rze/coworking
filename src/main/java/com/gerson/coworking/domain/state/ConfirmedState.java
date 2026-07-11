package com.gerson.coworking.domain.state;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.enums.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class ConfirmedState implements ReservationState {

    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public boolean canConfirm() {
        return false;
    }

    @Override
    public boolean canCancel() {
        return true;
    }

    @Override
    public Reservation confirm(Reservation reservation) {
        throw new IllegalStateException("Reservation is already confirmed");
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservation;
    }
}
