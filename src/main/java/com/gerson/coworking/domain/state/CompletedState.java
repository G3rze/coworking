package com.gerson.coworking.domain.state;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.enums.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class CompletedState implements ReservationState {

    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.COMPLETED;
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
    public boolean canComplete() {
        return false;
    }

    @Override
    public Reservation confirm(Reservation reservation) {
        throw new IllegalStateException("Cannot confirm a completed reservation");
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        throw new IllegalStateException("Cannot cancel a completed reservation");
    }

    @Override
    public Reservation complete(Reservation reservation) {
        throw new IllegalStateException("Reservation is already completed");
    }
}
