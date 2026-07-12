package com.gerson.coworking.exception;

public class OverlappingReservationException extends RuntimeException {

    public OverlappingReservationException(String message) {
        super(message);
    }

    public OverlappingReservationException() {
        super("Time slot conflict: space is already reserved for this time period");
    }
}
