package com.gerson.coworking.service;

import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationFilterRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationService {

    ReservationResponse create(UUID userId, ReservationCreateRequest request);

    ReservationResponse createForUser(UUID targetUserId, ReservationCreateRequest request);

    ReservationResponse confirm(UUID reservationId);

    ReservationResponse cancel(UUID reservationId);

    ReservationResponse complete(UUID reservationId);

    List<ReservationResponse> findAll();

    List<ReservationResponse> findByUser(UUID userId);

    Optional<ReservationResponse> findById(UUID id);

    List<ReservationResponse> filter(ReservationFilterRequest filter);

    boolean isUserAdmin(UUID userId);

    ReservationResponse findByIdForUser(UUID id, UUID userId, boolean isAdmin);

    ReservationResponse cancelForUser(UUID reservationId, UUID userId, boolean isAdmin);

    List<ReservationResponse> findByUserForUser(UUID targetUserId, UUID currentUserId, boolean isAdmin);

    List<ReservationResponse> filterForUser(ReservationFilterRequest filter, UUID userId, boolean isAdmin);

    ReservationResponse createReservation(UUID currentUserId, boolean isAdmin,
                                          UUID targetUserId, ReservationCreateRequest request);
}