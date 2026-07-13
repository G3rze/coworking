package com.gerson.coworking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerson.coworking.domain.dto.auth.LoginRequest;
import com.gerson.coworking.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin", "admin123");
        userToken = obtainToken("user", "user123");
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asText();
    }

    @Test
    @DisplayName("Should return occupancy report for date range - ADMIN access")
    void getOccupancyReport_AdminAccess_ReturnsReport() throws Exception {
        mockMvc.perform(get("/reports/occupancy")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateFrom").value("2026-07-01"))
                .andExpect(jsonPath("$.dateTo").value("2026-07-31"))
                .andExpect(jsonPath("$.totalDays").value(31))
                .andExpect(jsonPath("$.spaces").isArray())
                .andExpect(jsonPath("$.averageOccupancy").exists());
    }

    @Test
    @DisplayName("Should deny occupancy report access - USER role forbidden")
    void getOccupancyReport_UserAccess_Returns403() throws Exception {
        mockMvc.perform(get("/reports/occupancy")
                        .header("Authorization", "Bearer " + userToken)
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny occupancy report access - no auth")
    void getOccupancyReport_NoAuth_Returns401() throws Exception {
        mockMvc.perform(get("/reports/occupancy")
                        .param("dateFrom", "2026-07-01")
                        .param("dateTo", "2026-07-31"))
                .andExpect(status().isUnauthorized());
    }
}
