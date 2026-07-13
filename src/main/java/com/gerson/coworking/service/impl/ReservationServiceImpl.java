package com.gerson.coworking.service.impl;

import com.gerson.coworking.config.ZoneIdProvider;
import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationFilterRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;
import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.domain.enums.ReservationStatus;
import com.gerson.coworking.domain.event.ReservationEvent;
import com.gerson.coworking.exception.OverlappingReservationException;
import com.gerson.coworking.exception.ResourceNotFoundException;
import com.gerson.coworking.domain.mapper.ReservationMapper;
import com.gerson.coworking.domain.state.ReservationState;
import com.gerson.coworking.domain.state.ReservationStateFactory;
import com.gerson.coworking.repository.ReservationRepository;
import com.gerson.coworking.repository.SpaceRepository;
import com.gerson.coworking.repository.UserRepository;
import com.gerson.coworking.service.PaymentService;
import com.gerson.coworking.service.ReservationService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final ReservationStateFactory stateFactory;
    private final ZoneIdProvider zoneIdProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final Counter reservationsCreatedCounter;
    private final Counter reservationsConfirmedCounter;
    private final Counter reservationsCancelledCounter;
    private final Timer reservationCreationTimer;
    private final Timer reservationConfirmationTimer;
    private final AtomicLong activeReservationsGaugeValue;

    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                   SpaceRepository spaceRepository,
                                   UserRepository userRepository,
                                   PaymentService paymentService,
                                   ReservationStateFactory stateFactory,
                                   ZoneIdProvider zoneIdProvider,
                                   ApplicationEventPublisher eventPublisher,
                                   Counter reservationsCreatedCounter,
                                   Counter reservationsConfirmedCounter,
                                   Counter reservationsCancelledCounter,
                                   Timer reservationCreationTimer,
                                   Timer reservationConfirmationTimer,
                                   AtomicLong activeReservationsGaugeValue) {
        this.reservationRepository = reservationRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.stateFactory = stateFactory;
        this.zoneIdProvider = zoneIdProvider;
        this.eventPublisher = eventPublisher;
        this.reservationsCreatedCounter = reservationsCreatedCounter;
        this.reservationsConfirmedCounter = reservationsConfirmedCounter;
        this.reservationsCancelledCounter = reservationsCancelledCounter;
        this.reservationCreationTimer = reservationCreationTimer;
        this.reservationConfirmationTimer = reservationConfirmationTimer;
        this.activeReservationsGaugeValue = activeReservationsGaugeValue;
    }

    @Override
    @CacheEvict(value = "occupancy", allEntries = true)
    public ReservationResponse create(UUID userId, ReservationCreateRequest request) {
        return reservationCreationTimer.record(() -> {
            reservationsCreatedCounter.increment();

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
            return ReservationMapper.toResponse(saved, zoneIdProvider.getZoneId());
        });
    }

    @Override
    @CacheEvict(value = "occupancy", allEntries = true)
    public ReservationResponse confirm(UUID reservationId) {
        return reservationConfirmationTimer.record(() -> {
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
                reservationsConfirmedCounter.increment();
                activeReservationsGaugeValue.incrementAndGet();
            }

            Reservation updated = reservationRepository.save(reservation);
            eventPublisher.publishEvent(new ReservationEvent(this, reservation, ReservationStatus.PENDING_PAYMENT, reservation.getStatus()));
            return ReservationMapper.toResponse(updated, zoneIdProvider.getZoneId());
        });
    }

    @Override
    @CacheEvict(value = "occupancy", allEntries = true)
    public ReservationResponse cancel(UUID reservationId) {
        Reservation reservation = findByIdOrThrow(reservationId);
        ReservationState state = stateFactory.getState(reservation.getStatus());

        if (!state.canCancel()) {
            throw new IllegalStateException("Reservation cannot be cancelled in current state: " + reservation.getStatus());
        }

        ReservationStatus previousStatus = reservation.getStatus();
        state.cancel(reservation);

        if (previousStatus == ReservationStatus.CONFIRMED) {
            reservationsCancelledCounter.increment();
            activeReservationsGaugeValue.decrementAndGet();
        }

        Reservation updated = reservationRepository.save(reservation);
        eventPublisher.publishEvent(new ReservationEvent(this, reservation, previousStatus, ReservationStatus.CANCELLED));
        return ReservationMapper.toResponse(updated, zoneIdProvider.getZoneId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll().stream()
                .map(r -> ReservationMapper.toResponse(r, zoneIdProvider.getZoneId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> findByUser(UUID userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(r -> ReservationMapper.toResponse(r, zoneIdProvider.getZoneId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationResponse> findById(UUID id) {
        return reservationRepository.findById(id)
                .map(r -> ReservationMapper.toResponse(r, zoneIdProvider.getZoneId()));
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
                .map(r -> ReservationMapper.toResponse(r, zoneIdProvider.getZoneId()))
                .collect(Collectors.toList());
    }

    private Reservation findByIdOrThrow(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
    }
}