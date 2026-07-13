package com.gerson.coworking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerson.coworking.domain.dto.auth.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.gerson.coworking.TestcontainersConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("login_ValidAdminCredentials_ReturnsToken_200")
        void login_ValidAdminCredentials_ReturnsToken_200() throws Exception {
            LoginRequest request = new LoginRequest("admin", "admin123");

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.user.username").value("admin"))
                    .andExpect(jsonPath("$.user.role").value("ADMIN"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("token");
        }

        @Test
        @DisplayName("login_ValidUserCredentials_ReturnsToken_200")
        void login_ValidUserCredentials_ReturnsToken_200() throws Exception {
            LoginRequest request = new LoginRequest("user", "user123");

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.username").value("user"))
                    .andExpect(jsonPath("$.user.role").value("USER"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("token");
        }

        @Test
        @DisplayName("login_InvalidUsername_Returns400")
        void login_InvalidUsername_Returns400() throws Exception {
            LoginRequest request = new LoginRequest("nonexistent", "admin123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        @DisplayName("login_InvalidPassword_Returns400")
        void login_InvalidPassword_Returns400() throws Exception {
            LoginRequest request = new LoginRequest("admin", "wrongpassword");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        @DisplayName("login_MissingUsername_Returns400")
        void login_MissingUsername_Returns400() throws Exception {
            LoginRequest request = new LoginRequest(null, "admin123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.username").exists());
        }

        @Test
        @DisplayName("login_MissingPassword_Returns400")
        void login_MissingPassword_Returns400() throws Exception {
            LoginRequest request = new LoginRequest("admin", null);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.password").exists());
        }

        @Test
        @DisplayName("login_BlankUsername_Returns400")
        void login_BlankUsername_Returns400() throws Exception {
            LoginRequest request = new LoginRequest("  ", "admin123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.username").exists());
        }
    }
}
