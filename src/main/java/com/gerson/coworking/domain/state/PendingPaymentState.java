package com.gerson.coworking.domain.state;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.enums.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class PendingPaymentState implements ReservationState {

    @Override
    public ReservationStatus getStatus() {
        return ReservationStatus.PENDING_PAYMENT;
    }

    @Override
    public boolean canConfirm() {
        return true;
    }

    @Override
    public boolean canCancel() {
        return true;
    }

    @Override
    public boolean canComplete() {
        return false;
    }

    @Override
    public Reservation confirm(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservation;
    }

    @Override
    public Reservation cancel(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservation;
    }

    @Override
    public Reservation complete(Reservation reservation) {
        throw new IllegalStateException("Cannot complete a reservation that is not confirmed");
    }
}
