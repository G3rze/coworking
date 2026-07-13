package com.gerson.coworking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerson.coworking.domain.dto.auth.LoginRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.TestcontainersConfiguration;
import com.gerson.coworking.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
class NotificationEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private String adminToken;
    private String userToken;

    private static final UUID EXISTING_SPACE_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID REGULAR_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

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

    @Test
    @DisplayName("Should publish event and trigger notification on reservation confirmation")
    void confirmReservation_shouldTriggerNotification() throws Exception {
        ReservationCreateRequest createRequest = new ReservationCreateRequest(
                EXISTING_SPACE_ID,
                LocalDate.now().plusDays(1),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String reservationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Should publish event and trigger notification on reservation cancellation")
    void cancelReservation_shouldTriggerNotification() throws Exception {
        ReservationCreateRequest createRequest = new ReservationCreateRequest(
                EXISTING_SPACE_ID,
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String reservationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/v1/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
