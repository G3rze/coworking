package com.gerson.coworking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerson.coworking.domain.dto.auth.LoginRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.gerson.coworking.TestcontainersConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private String adminToken;
    private String userToken;

    private static final UUID ADMIN_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID REGULAR_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID EXISTING_SPACE_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID NON_EXISTING_SPACE_ID = UUID.fromString("999e8400-e29b-41d4-a716-446655440999");
    private static final UUID NON_EXISTING_RESERVATION_ID = UUID.fromString("999e8400-e29b-41d4-a716-446655440999");

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin", "admin123");
        userToken = obtainToken("user", "user123");

        when(paymentService.validatePayment(any(UUID.class), any(BigDecimal.class)))
                .thenReturn(new PaymentService.PaymentValidationResult(true, "Payment successful"));
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asText();
    }

    @Nested
    @DisplayName("GET /api/v1/reservations")
    class GetAllReservations {

        @Test
        @DisplayName("getAllReservations_WithAdminToken_Returns200")
        void getAllReservations_WithAdminToken_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/reservations")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("getAllReservations_WithUserToken_Returns403")
        void getAllReservations_WithUserToken_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/reservations")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("getAllReservations_WithoutToken_Returns401")
        void getAllReservations_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/reservations"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reservations/{id}")
    class GetReservationById {

        @Test
        @DisplayName("getReservationById_NonExistingId_Returns404")
        void getReservationById_NonExistingId_Returns404() throws Exception {
            mockMvc.perform(get("/api/v1/reservations/{id}", NON_EXISTING_RESERVATION_ID)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reservations/user/{userId}")
    class GetReservationsByUser {

        @Test
        @DisplayName("getReservationsByUser_ExistingUser_Returns200")
        void getReservationsByUser_ExistingUser_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/reservations/user/{userId}", REGULAR_USER_ID)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("getReservationsByUser_NonExistingUser_Returns200")
        void getReservationsByUser_NonExistingUser_Returns200() throws Exception {
            UUID nonExistingUserId = UUID.fromString("999e8400-e29b-41d4-a716-446655440999");
            mockMvc.perform(get("/api/v1/reservations/user/{userId}", nonExistingUserId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/reservations")
    class CreateReservation {

        @Test
        @DisplayName("createReservation_Success_Returns201")
        void createReservation_Success_Returns201() throws Exception {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    EXISTING_SPACE_ID,
                    LocalDate.now().plusDays(5),
                    LocalTime.of(14, 0),
                    LocalTime.of(17, 0)
            );

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                    .andExpect(jsonPath("$.space.id").value(EXISTING_SPACE_ID.toString()));
        }

        @Test
        @DisplayName("createReservation_SpaceNotFound_Returns404")
        void createReservation_SpaceNotFound_Returns404() throws Exception {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    NON_EXISTING_SPACE_ID,
                    LocalDate.now().plusDays(5),
                    LocalTime.of(14, 0),
                    LocalTime.of(17, 0)
            );

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("createReservation_EndTimeBeforeStartTime_Returns400")
        void createReservation_EndTimeBeforeStartTime_Returns400() throws Exception {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    EXISTING_SPACE_ID,
                    LocalDate.now().plusDays(5),
                    LocalTime.of(17, 0),
                    LocalTime.of(14, 0)
            );

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("End time must be after start time"));
        }

        @Test
        @DisplayName("createReservation_WithoutAuth_Returns401")
        void createReservation_WithoutAuth_Returns401() throws Exception {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    EXISTING_SPACE_ID,
                    LocalDate.now().plusDays(5),
                    LocalTime.of(14, 0),
                    LocalTime.of(17, 0)
            );

            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("createReservation_Overlapping_Returns409")
        void createReservation_Overlapping_Returns409() throws Exception {
            LocalDate futureDate = LocalDate.now().plusDays(10);
            ReservationCreateRequest request1 = new ReservationCreateRequest(
                    EXISTING_SPACE_ID,
                    futureDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(12, 0)
            );

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            ReservationCreateRequest request2 = new ReservationCreateRequest(
                    EXISTING_SPACE_ID,
                    futureDate,
                    LocalTime.of(10, 0),
                    LocalTime.of(13, 0)
            );

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/reservations/{id}/confirm")
    class ConfirmReservation {

        @Test
        @DisplayName("confirmReservation_NonExisting_Returns404")
        void confirmReservation_NonExisting_Returns404() throws Exception {
            mockMvc.perform(post("/api/v1/reservations/{id}/confirm", NON_EXISTING_RESERVATION_ID)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/reservations/{id}/cancel")
    class CancelReservation {

        @Test
        @DisplayName("cancelReservation_NonExisting_Returns404")
        void cancelReservation_NonExisting_Returns404() throws Exception {
            mockMvc.perform(post("/api/v1/reservations/{id}/cancel", NON_EXISTING_RESERVATION_ID)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("Full Reservation Flow")
    class FullReservationFlow {

        @Test
        @DisplayName("create_Confirm_Cancel_FullFlow_Returns200")
        void create_Confirm_Cancel_FullFlow_Returns200() throws Exception {
            LocalDate futureDate = LocalDate.now().plusDays(15);
            ReservationCreateRequest createRequest = new ReservationCreateRequest(
                    EXISTING_SPACE_ID,
                    futureDate,
                    LocalTime.of(8, 0),
                    LocalTime.of(10, 0)
            );

            MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                    .andReturn();

            String createResponseBody = createResult.getResponse().getContentAsString();
            UUID reservationId = UUID.fromString(objectMapper.readTree(createResponseBody).get("id").asText());

            mockMvc.perform(post("/api/v1/reservations/{id}/confirm", reservationId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            mockMvc.perform(post("/api/v1/reservations/{id}/cancel", reservationId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }
}
