package com.gerson.coworking.domain.dto.reservation;

import com.gerson.coworking.domain.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private UUID id;
    private SpaceBasicInfo space;
    private UserBasicInfo user;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    private String paymentReference;
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpaceBasicInfo {
        private UUID id;
        private String name;
        private String location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserBasicInfo {
        private UUID id;
        private String username;
        private String email;
    }
}
