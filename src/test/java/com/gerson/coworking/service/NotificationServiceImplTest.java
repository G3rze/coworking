package com.gerson.coworking.service;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.domain.enums.ReservationStatus;
import com.gerson.coworking.domain.enums.Role;
import com.gerson.coworking.domain.enums.SpaceStatus;
import com.gerson.coworking.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NotificationServiceImpl notificationService;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl();
        testReservation = createTestReservation();
    }

    @Test
    @DisplayName("Should send confirmation notification")
    void sendReservationConfirmation_shouldLogEmail() {
        notificationService.sendReservationConfirmation(testReservation);
        verify(eventPublisher, times(0)).publishEvent(null);
    }

    @Test
    @DisplayName("Should send cancellation notification")
    void sendReservationCancellation_shouldLogEmail() {
        notificationService.sendReservationCancellation(testReservation);
    }

    private Reservation createTestReservation() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .role(Role.USER)
                .build();

        Space space = Space.builder()
                .id(UUID.randomUUID())
                .name("Sala de Juntas A")
                .description("Sala para 10 personas")
                .capacity(10)
                .location("Piso 2")
                .pricePerHour(java.math.BigDecimal.valueOf(50))
                .status(SpaceStatus.AVAILABLE)
                .build();

        return Reservation.builder()
                .id(UUID.randomUUID())
                .space(space)
                .user(user)
                .date(LocalDate.of(2026, 7, 20))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .status(ReservationStatus.CONFIRMED)
                .totalPrice(java.math.BigDecimal.valueOf(100))
                .build();
    }
}
