package com.gerson.coworking.domain.dto.reservation;

import com.gerson.coworking.domain.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationFilterRequest {

    private UUID spaceId;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private ReservationStatus status;
}
