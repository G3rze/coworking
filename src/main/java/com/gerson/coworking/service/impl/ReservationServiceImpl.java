package com.gerson.coworking.service.impl;

import com.gerson.coworking.config.ZoneIdProvider;
import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationFilterRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;
import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.domain.enums.ReservationStatus;
import com.gerson.coworking.domain.enums.Role;
import com.gerson.coworking.domain.event.ReservationEvent;
import com.gerson.coworking.exception.OverlappingReservationException;
import com.gerson.coworking.exception.ResourceNotFoundException;
import com.gerson.coworking.domain.mapper.ReservationMapper;
import com.gerson.coworking.domain.state.ReservationState;
import com.gerson.coworking.domain.state.ReservationStateFactory;
import com.gerson.coworking.domain.strategy.PricingStrategy;
import com.gerson.coworking.repository.ReservationRepository;
import com.gerson.coworking.repository.SpaceRepository;
import com.gerson.coworking.repository.UserRepository;
import com.gerson.coworking.service.PaymentService;
import com.gerson.coworking.service.ReservationService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private final PricingStrategy pricingStrategy;
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
                                   PricingStrategy pricingStrategy,
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
        this.pricingStrategy = pricingStrategy;
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

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            if (user.getRole() == com.gerson.coworking.domain.enums.Role.ADMIN) {
                throw new IllegalArgumentException("No se pueden crear reservas para administradores");
            }

            Space space = spaceRepository.findById(request.spaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Space", "id", request.spaceId()));

            validateTimeRange(request);
            validateNoOverlap(request);

            BigDecimal totalPrice = pricingStrategy.calculatePrice(
                    space.getPricePerHour(),
                    request.startTime(),
                    request.endTime()
            );

            Reservation reservation = Reservation.builder()
                    .space(space)
                    .user(user)
                    .date(request.date())
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .status(ReservationStatus.PENDING_PAYMENT)
                    .totalPrice(totalPrice)
                    .build();

            Reservation saved = reservationRepository.save(reservation);
            return ReservationMapper.toResponse(saved, zoneIdProvider.getZoneId());
        });
    }

    @Override
    @CacheEvict(value = "occupancy", allEntries = true)
    public ReservationResponse createForUser(UUID targetUserId, ReservationCreateRequest request) {
        return reservationCreationTimer.record(() -> {
            reservationsCreatedCounter.increment();

            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

            if (targetUser.getRole() == com.gerson.coworking.domain.enums.Role.ADMIN) {
                throw new IllegalArgumentException("No se pueden crear reservas para administradores");
            }

            Space space = spaceRepository.findById(request.spaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Space", "id", request.spaceId()));

            validateTimeRange(request);
            validateNoOverlap(request);

            BigDecimal totalPrice = pricingStrategy.calculatePrice(
                    space.getPricePerHour(),
                    request.startTime(),
                    request.endTime()
            );

            Reservation reservation = Reservation.builder()
                    .space(space)
                    .user(targetUser)
                    .date(request.date())
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .status(ReservationStatus.PENDING_PAYMENT)
                    .totalPrice(totalPrice)
                    .build();

            Reservation saved = reservationRepository.save(reservation);
            return ReservationMapper.toResponse(saved, zoneIdProvider.getZoneId());
        });
    }

    @Override
    public ReservationResponse createReservation(UUID currentUserId, boolean isAdmin,
                                                 UUID targetUserId, ReservationCreateRequest request) {
        if (isAdmin) {
            if (targetUserId == null) {
                throw new IllegalArgumentException("Admin no puede tener reservas");
            }
            if (isUserAdmin(targetUserId)) {
                throw new IllegalArgumentException("No se pueden crear reservas para administradores");
            }
            return createForUser(targetUserId, request);
        } else {
            if (targetUserId != null) {
                throw new IllegalArgumentException("No puedes especificar userId en tu solicitud");
            }
            return create(currentUserId, request);
        }
    }

    @Override
    public ReservationResponse findByIdForUser(UUID id, UUID userId, boolean isAdmin) {
        ReservationResponse reservation = reservationRepository.findById(id)
                .map(r -> ReservationMapper.toResponse(r, zoneIdProvider.getZoneId()))
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        if (!isAdmin && !reservation.user().id().equals(userId)) {
            throw new AccessDeniedException("No puedes acceder a esta reserva");
        }
        return reservation;
    }

    @Override
    public ReservationResponse cancelForUser(UUID reservationId, UUID userId, boolean isAdmin) {
        if (!isAdmin) {
            findByIdForUser(reservationId, userId, false);
        }
        return cancel(reservationId);
    }

    @Override
    public List<ReservationResponse> findByUserForUser(UUID targetUserId, UUID currentUserId, boolean isAdmin) {
        if (!isAdmin && !targetUserId.equals(currentUserId)) {
            throw new AccessDeniedException("Solo puedes ver tus propias reservas");
        }
        return findByUser(targetUserId);
    }

    @Override
    public List<ReservationResponse> filterForUser(ReservationFilterRequest filter, UUID userId, boolean isAdmin) {
        List<ReservationResponse> results = filter(filter);
        if (!isAdmin) {
            return results.stream()
                    .filter(r -> r.user().id().equals(userId))
                    .toList();
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
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

        Reservation updated = reservationRepository.save(reservation);
        eventPublisher.publishEvent(new ReservationEvent(this, reservation, previousStatus, ReservationStatus.CANCELLED));
        return ReservationMapper.toResponse(updated, zoneIdProvider.getZoneId());
    }

    @Override
    @CacheEvict(value = "occupancy", allEntries = true)
    public ReservationResponse complete(UUID reservationId) {
        Reservation reservation = findByIdOrThrow(reservationId);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can be completed");
        }

        if (!isReservationInPast(reservation)) {
            throw new IllegalStateException("Cannot complete reservation before it ends");
        }

        ReservationState state = stateFactory.getState(reservation.getStatus());
        if (!state.canComplete()) {
            throw new IllegalStateException("Reservation cannot be completed in current state: " + reservation.getStatus());
        }

        ReservationStatus previousStatus = reservation.getStatus();
        state.complete(reservation);

        Reservation updated = reservationRepository.save(reservation);
        eventPublisher.publishEvent(new ReservationEvent(this, reservation, previousStatus, ReservationStatus.COMPLETED));
        return ReservationMapper.toResponse(updated, zoneIdProvider.getZoneId());
    }

    private boolean isReservationInPast(Reservation reservation) {
        LocalDate today = LocalDate.now(zoneIdProvider.getZoneId());
        LocalTime now = LocalTime.now(zoneIdProvider.getZoneId());

        if (reservation.getDate().isBefore(today)) {
            return true;
        }
        if (reservation.getDate().isEqual(today)) {
            return reservation.getEndTime().isBefore(now) || reservation.getEndTime().equals(now);
        }
        return false;
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

    private void validateTimeRange(ReservationCreateRequest request) {
        if (request.endTime().isBefore(request.startTime()) ||
            request.endTime().equals(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    private void validateNoOverlap(ReservationCreateRequest request) {
        boolean hasConflict = reservationRepository.existsConflictingReservation(
                request.spaceId(),
                request.date(),
                request.startTime(),
                request.endTime()
        );

        if (hasConflict) {
            throw new OverlappingReservationException();
        }
    }
}