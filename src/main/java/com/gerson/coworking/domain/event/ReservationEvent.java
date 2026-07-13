package com.gerson.coworking.domain.event;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.enums.ReservationStatus;
import org.springframework.context.ApplicationEvent;

public class ReservationEvent extends ApplicationEvent {

    private final Reservation reservation;
    private final ReservationStatus previousStatus;
    private final ReservationStatus newStatus;

    public ReservationEvent(Object source, Reservation reservation,
                            ReservationStatus previousStatus, ReservationStatus newStatus) {
        super(source);
        this.reservation = reservation;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public ReservationStatus getPreviousStatus() {
        return previousStatus;
    }

    public ReservationStatus getNewStatus() {
        return newStatus;
    }
}
