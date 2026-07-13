package com.gerson.coworking.service;

import com.gerson.coworking.config.ZoneIdProvider;
import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;
import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.domain.enums.ReservationStatus;
import com.gerson.coworking.domain.enums.Role;
import com.gerson.coworking.domain.mapper.ReservationMapper;
import com.gerson.coworking.domain.state.ReservationState;
import com.gerson.coworking.domain.state.ReservationStateFactory;
import com.gerson.coworking.exception.OverlappingReservationException;
import com.gerson.coworking.exception.ResourceNotFoundException;
import com.gerson.coworking.repository.ReservationRepository;
import com.gerson.coworking.repository.SpaceRepository;
import com.gerson.coworking.repository.UserRepository;
import com.gerson.coworking.service.impl.ReservationServiceImpl;
import com.gerson.coworking.service.impl.PaymentServiceImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ReservationStateFactory stateFactory;

    @Mock
    private ZoneIdProvider zoneIdProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Counter reservationsCreatedCounter;

    @Mock
    private Counter reservationsConfirmedCounter;

    @Mock
    private Counter reservationsCancelledCounter;

    @Mock
    private Timer reservationCreationTimer;

    @Mock
    private Timer reservationConfirmationTimer;

    private AtomicLong activeReservationsGaugeValue;

    private ReservationServiceImpl reservationService;

    private Space testSpace;
    private User testUser;
    private Reservation testReservation;
    private ReservationResponse testReservationResponse;

    private final UUID testSpaceId = UUID.randomUUID();
    private final UUID testUserId = UUID.randomUUID();
    private final UUID testReservationId = UUID.randomUUID();
    private final ZoneId testZone = ZoneId.of("America/El_Salvador");

    @BeforeEach
    void setUp() {
        activeReservationsGaugeValue = new AtomicLong(0);

        lenient().when(reservationCreationTimer.record(any(java.util.function.Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        lenient().when(reservationConfirmationTimer.record(any(java.util.function.Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        reservationService = new ReservationServiceImpl(
                reservationRepository,
                spaceRepository,
                userRepository,
                paymentService,
                stateFactory,
                zoneIdProvider,
                eventPublisher,
                reservationsCreatedCounter,
                reservationsConfirmedCounter,
                reservationsCancelledCounter,
                reservationCreationTimer,
                reservationConfirmationTimer,
                activeReservationsGaugeValue
        );

        testSpace = Space.builder()
                .id(testSpaceId)
                .name("Conference Room A")
                .description("Large meeting room")
                .capacity(10)
                .location("Floor 2")
                .pricePerHour(new BigDecimal("25.00"))
                .status(com.gerson.coworking.domain.enums.SpaceStatus.AVAILABLE)
                .build();

        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
                .role(Role.USER)
                .build();

        testReservation = Reservation.builder()
                .id(testReservationId)
                .space(testSpace)
                .user(testUser)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .status(ReservationStatus.PENDING_PAYMENT)
                .totalPrice(new BigDecimal("75.00"))
                .build();
        testReservation.setCreatedAt(Instant.now());

        testReservationResponse = ReservationMapper.toResponse(testReservation, testZone);
    }

    @Nested
    @DisplayName("create_Success scenarios")
    class CreateSuccess {

        @Test
        @DisplayName("create_Success_ReturnsCreatedReservation")
        void create_Success_ReturnsCreatedReservation() {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    testSpaceId,
                    LocalDate.now().plusDays(1),
                    LocalTime.of(9, 0),
                    LocalTime.of(12, 0)
            );

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findById(testSpaceId)).thenReturn(Optional.of(testSpace));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(reservationRepository.existsConflictingReservation(any(), any(), any(), any())).thenReturn(false);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

            ReservationResponse result = reservationService.create(testUserId, request);

            assertThat(result).isNotNull();
            assertThat(result.space().id()).isEqualTo(testSpaceId);
            verify(reservationRepository, times(1)).save(any(Reservation.class));
        }
    }

    @Nested
    @DisplayName("create_Failure scenarios")
    class CreateFailure {

        @Test
        @DisplayName("create_SpaceNotFound_ThrowsResourceNotFoundException")
        void create_SpaceNotFound_ThrowsResourceNotFoundException() {
            UUID nonExistentSpaceId = UUID.randomUUID();
            ReservationCreateRequest request = new ReservationCreateRequest(
                    nonExistentSpaceId,
                    LocalDate.now().plusDays(1),
                    LocalTime.of(9, 0),
                    LocalTime.of(12, 0)
            );

            when(spaceRepository.findById(nonExistentSpaceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.create(testUserId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Space");
        }

        @Test
        @DisplayName("create_UserNotFound_ThrowsResourceNotFoundException")
        void create_UserNotFound_ThrowsResourceNotFoundException() {
            UUID nonExistentUserId = UUID.randomUUID();
            ReservationCreateRequest request = new ReservationCreateRequest(
                    testSpaceId,
                    LocalDate.now().plusDays(1),
                    LocalTime.of(9, 0),
                    LocalTime.of(12, 0)
            );

            when(spaceRepository.findById(testSpaceId)).thenReturn(Optional.of(testSpace));
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.create(nonExistentUserId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }

        @Test
        @DisplayName("create_EndTimeBeforeStartTime_ThrowsIllegalArgumentException")
        void create_EndTimeBeforeStartTime_ThrowsIllegalArgumentException() {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    testSpaceId,
                    LocalDate.now().plusDays(1),
                    LocalTime.of(14, 0),
                    LocalTime.of(10, 0)
            );

            when(spaceRepository.findById(testSpaceId)).thenReturn(Optional.of(testSpace));

            assertThatThrownBy(() -> reservationService.create(testUserId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End time must be after start time");
        }

        @Test
        @DisplayName("create_EndTimeEqualsStartTime_ThrowsIllegalArgumentException")
        void create_EndTimeEqualsStartTime_ThrowsIllegalArgumentException() {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    testSpaceId,
                    LocalDate.now().plusDays(1),
                    LocalTime.of(10, 0),
                    LocalTime.of(10, 0)
            );

            when(spaceRepository.findById(testSpaceId)).thenReturn(Optional.of(testSpace));

            assertThatThrownBy(() -> reservationService.create(testUserId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End time must be after start time");
        }

        @Test
        @DisplayName("create_OverlappingReservation_ThrowsOverlappingReservationException")
        void create_OverlappingReservation_ThrowsOverlappingReservationException() {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    testSpaceId,
                    LocalDate.now().plusDays(1),
                    LocalTime.of(9, 0),
                    LocalTime.of(12, 0)
            );

            when(spaceRepository.findById(testSpaceId)).thenReturn(Optional.of(testSpace));
            when(reservationRepository.existsConflictingReservation(any(), any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(testUserId, request))
                    .isInstanceOf(OverlappingReservationException.class);
        }
    }

    @Nested
    @DisplayName("confirm_Success scenarios")
    class ConfirmSuccess {

        @Test
        @DisplayName("confirm_Success_FromPendingPayment_StatusChangesToConfirmed")
        void confirm_Success_FromPendingPayment_StatusChangesToConfirmed() {
            ReservationState pendingState = mock(ReservationState.class);
            when(pendingState.canConfirm()).thenReturn(true);
            when(pendingState.confirm(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation r = invocation.getArgument(0);
                r.setStatus(ReservationStatus.CONFIRMED);
                return r;
            });

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(stateFactory.getState(ReservationStatus.PENDING_PAYMENT)).thenReturn(pendingState);
            when(reservationRepository.findById(testReservationId)).thenReturn(Optional.of(testReservation));
            when(paymentService.validatePayment(any(), any())).thenReturn(new PaymentServiceImpl.PaymentValidationResult(true, "Success"));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReservationResponse result = reservationService.confirm(testReservationId);

            assertThat(result).isNotNull();
            verify(pendingState, times(1)).confirm(any(Reservation.class));
        }
    }

    @Nested
    @DisplayName("confirm_Failure scenarios")
    class ConfirmFailure {

        @Test
        @DisplayName("confirm_NotFound_ThrowsResourceNotFoundException")
        void confirm_NotFound_ThrowsResourceNotFoundException() {
            UUID nonExistentId = UUID.randomUUID();
            when(reservationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.confirm(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation");
        }

        @Test
        @DisplayName("confirm_CannotConfirmAlreadyConfirmed_ThrowsIllegalStateException")
        void confirm_CannotConfirmAlreadyConfirmed_ThrowsIllegalStateException() {
            testReservation.setStatus(ReservationStatus.CONFIRMED);

            ReservationState confirmedState = mock(ReservationState.class);
            when(confirmedState.canConfirm()).thenReturn(false);
            when(stateFactory.getState(ReservationStatus.CONFIRMED)).thenReturn(confirmedState);
            when(reservationRepository.findById(testReservationId)).thenReturn(Optional.of(testReservation));

            assertThatThrownBy(() -> reservationService.confirm(testReservationId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot be confirmed");
        }

        @Test
        @DisplayName("confirm_CancelledReservation_ThrowsIllegalStateException")
        void confirm_CancelledReservation_ThrowsIllegalStateException() {
            testReservation.setStatus(ReservationStatus.CANCELLED);

            ReservationState cancelledState = mock(ReservationState.class);
            when(cancelledState.canConfirm()).thenReturn(false);
            when(stateFactory.getState(ReservationStatus.CANCELLED)).thenReturn(cancelledState);
            when(reservationRepository.findById(testReservationId)).thenReturn(Optional.of(testReservation));

            assertThatThrownBy(() -> reservationService.confirm(testReservationId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Reservation cannot be confirmed in current state: CANCELLED");
        }
    }

    @Nested
    @DisplayName("cancel_Success scenarios")
    class CancelSuccess {

        @Test
        @DisplayName("cancel_Success_FromPendingPayment_StatusChangesToCancelled")
        void cancel_Success_FromPendingPayment_StatusChangesToCancelled() {
            ReservationState pendingState = mock(ReservationState.class);
            when(pendingState.canCancel()).thenReturn(true);
            when(pendingState.cancel(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation r = invocation.getArgument(0);
                r.setStatus(ReservationStatus.CANCELLED);
                return r;
            });

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(stateFactory.getState(ReservationStatus.PENDING_PAYMENT)).thenReturn(pendingState);
            when(reservationRepository.findById(testReservationId)).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReservationResponse result = reservationService.cancel(testReservationId);

            assertThat(result).isNotNull();
            verify(pendingState, times(1)).cancel(any(Reservation.class));
        }

        @Test
        @DisplayName("cancel_Success_FromConfirmed_StatusChangesToCancelled")
        void cancel_Success_FromConfirmed_StatusChangesToCancelled() {
            testReservation.setStatus(ReservationStatus.CONFIRMED);

            ReservationState confirmedState = mock(ReservationState.class);
            when(confirmedState.canCancel()).thenReturn(true);
            when(confirmedState.cancel(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation r = invocation.getArgument(0);
                r.setStatus(ReservationStatus.CANCELLED);
                return r;
            });

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(stateFactory.getState(ReservationStatus.CONFIRMED)).thenReturn(confirmedState);
            when(reservationRepository.findById(testReservationId)).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReservationResponse result = reservationService.cancel(testReservationId);

            assertThat(result).isNotNull();
            verify(confirmedState, times(1)).cancel(any(Reservation.class));
        }
    }

    @Nested
    @DisplayName("cancel_Failure scenarios")
    class CancelFailure {

        @Test
        @DisplayName("cancel_CannotCancelAlreadyCancelled_ThrowsIllegalStateException")
        void cancel_CannotCancelAlreadyCancelled_ThrowsIllegalStateException() {
            testReservation.setStatus(ReservationStatus.CANCELLED);

            ReservationState cancelledState = mock(ReservationState.class);
            when(cancelledState.canCancel()).thenReturn(false);
            when(stateFactory.getState(ReservationStatus.CANCELLED)).thenReturn(cancelledState);
            when(reservationRepository.findById(testReservationId)).thenReturn(Optional.of(testReservation));

            assertThatThrownBy(() -> reservationService.cancel(testReservationId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Reservation cannot be cancelled in current state: CANCELLED");
        }
    }

    @Nested
    @DisplayName("findAll_Success scenarios")
    class FindAllSuccess {

        @Test
        @DisplayName("findAll_ReturnsAllReservations")
        void findAll_ReturnsAllReservations() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(reservationRepository.findAll()).thenReturn(List.of(testReservation));

            List<ReservationResponse> result = reservationService.findAll();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByUser_Success scenarios")
    class FindByUserSuccess {

        @Test
        @DisplayName("findByUser_ReturnsUserReservations")
        void findByUser_ReturnsUserReservations() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(reservationRepository.findByUserId(testUserId)).thenReturn(List.of(testReservation));

            List<ReservationResponse> result = reservationService.findByUser(testUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).user().id()).isEqualTo(testUserId);
        }
    }

    @Nested
    @DisplayName("findById_Success scenarios")
    class FindByIdSuccess {

        @Test
        @DisplayName("findById_ExistingId_ReturnsReservation")
        void findById_ExistingId_ReturnsReservation() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(reservationRepository.findById(testReservationId)).thenReturn(Optional.of(testReservation));

            Optional<ReservationResponse> result = reservationService.findById(testReservationId);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(testReservationId);
        }
    }
}
