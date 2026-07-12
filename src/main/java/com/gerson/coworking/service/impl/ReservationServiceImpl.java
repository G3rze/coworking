package com.gerson.coworking.service.impl;

import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationFilterRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;
import com.gerson.coworking.domain.dto.reservation.SpaceBasicInfo;
import com.gerson.coworking.domain.dto.reservation.UserBasicInfo;
import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.domain.enums.ReservationStatus;
import com.gerson.coworking.domain.state.ReservationState;
import com.gerson.coworking.domain.state.ReservationStateFactory;
import com.gerson.coworking.exception.OverlappingReservationException;
import com.gerson.coworking.exception.ResourceNotFoundException;
import com.gerson.coworking.repository.ReservationRepository;
import com.gerson.coworking.repository.SpaceRepository;
import com.gerson.coworking.repository.UserRepository;
import com.gerson.coworking.service.PaymentService;
import com.gerson.coworking.service.ReservationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final ReservationStateFactory stateFactory;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                   SpaceRepository spaceRepository,
                                   UserRepository userRepository,
                                   PaymentService paymentService,
                                   ReservationStateFactory stateFactory) {
        this.reservationRepository = reservationRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.stateFactory = stateFactory;
    }

    @Override
    public ReservationResponse create(UUID userId, ReservationCreateRequest request) {
        Space space = spaceRepository.findById(request.spaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Space", "id", request.spaceId()));

        if (request.endTime().isBefore(request.startTime()) ||
            request.endTime().equals(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        boolean hasConflict = reservationRepository.existsConflictingReservation(
                request.spaceId(),
                request.date(),
                request.startTime(),
                request.endTime()
        );

        if (hasConflict) {
            throw new OverlappingReservationException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Reservation reservation = Reservation.builder()
                .space(space)
                .user(user)
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return mapToResponse(saved);
    }

    @Override
    public ReservationResponse confirm(UUID reservationId) {
        Reservation reservation = findByIdOrThrow(reservationId);
        ReservationState state = stateFactory.getState(reservation.getStatus());

        if (!state.canConfirm()) {
            throw new IllegalStateException("Reservation cannot be confirmed in current state: " + reservation.getStatus());
        }

        PaymentService.PaymentValidationResult result = paymentService.validatePayment(
                reservationId,
                reservation.getTotalPrice()
        );

        if (result.success()) {
            state.confirm(reservation);
        }

        Reservation updated = reservationRepository.save(reservation);
        return mapToResponse(updated);
    }

    @Override
    public ReservationResponse cancel(UUID reservationId) {
        Reservation reservation = findByIdOrThrow(reservationId);
        ReservationState state = stateFactory.getState(reservation.getStatus());

        if (!state.canCancel()) {
            throw new IllegalStateException("Reservation cannot be cancelled in current state: " + reservation.getStatus());
        }

        state.cancel(reservation);
        Reservation updated = reservationRepository.save(reservation);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> findByUser(UUID userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationResponse> findById(UUID id) {
        return reservationRepository.findById(id)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> filter(ReservationFilterRequest filter) {
        return reservationRepository.findByFilter(
                filter.spaceId(),
                filter.dateFrom(),
                filter.dateTo(),
                filter.status()
        ).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Reservation findByIdOrThrow(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        SpaceBasicInfo spaceBasicInfo = new SpaceBasicInfo(
                reservation.getSpace().getId(),
                reservation.getSpace().getName(),
                reservation.getSpace().getLocation()
        );

        UserBasicInfo userBasicInfo = new UserBasicInfo(
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getUser().getEmail()
        );

        return new ReservationResponse(
                reservation.getId(),
                spaceBasicInfo,
                userBasicInfo,
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getTotalPrice(),
                reservation.getPaymentReference(),
                reservation.getCreatedAt()
        );
    }
}
