package com.gerson.coworking.service;

import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationFilterRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationService {

    ReservationResponse create(UUID userId, ReservationCreateRequest request);

    ReservationResponse confirm(UUID reservationId);

    ReservationResponse cancel(UUID reservationId);

    List<ReservationResponse> findAll();

    List<ReservationResponse> findByUser(UUID userId);

    Optional<ReservationResponse> findById(UUID id);

    List<ReservationResponse> filter(ReservationFilterRequest filter);
}
