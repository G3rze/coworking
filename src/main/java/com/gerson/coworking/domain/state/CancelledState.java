package com.gerson.coworking.domain.state;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.enums.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class CancelledState implements ReservationState {

    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.CANCELLED;
    }

    @Override
    public boolean canConfirm() {
        return false;
    }

    @Override
    public boolean canCancel() {
        return false;
    }

    @Override
    public Reservation confirm(Reservation reservation) {
        throw new IllegalStateException("Cannot confirm a cancelled reservation");
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        throw new IllegalStateException("Reservation is already cancelled");
    }
}
