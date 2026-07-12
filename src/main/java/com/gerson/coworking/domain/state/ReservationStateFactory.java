package com.gerson.coworking.domain.state;

import com.gerson.coworking.domain.enums.ReservationStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReservationStateFactory {

    private final Map<ReservationStatus, ReservationState> states;

    public ReservationStateFactory(PendingPaymentState pendingState,
                                  ConfirmedState confirmedState,
                                  CancelledState cancelledState) {
        this.states = Map.of(
                ReservationStatus.PENDING_PAYMENT, pendingState,
                ReservationStatus.CONFIRMED, confirmedState,
                ReservationStatus.CANCELLED, cancelledState
        );
    }

    public ReservationState getState(ReservationStatus status) {
        ReservationState state = states.get(status);
        if (state == null) {
            throw new IllegalArgumentException("Unknown reservation status: " + status);
        }
        return state;
    }
}
