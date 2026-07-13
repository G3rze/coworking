package com.gerson.coworking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerson.coworking.domain.dto.auth.LoginRequest;
import com.gerson.coworking.domain.dto.space.SpaceCreateRequest;
import com.gerson.coworking.domain.dto.space.SpaceUpdateRequest;
import com.gerson.coworking.domain.enums.SpaceStatus;
import com.gerson.coworking.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SpaceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpaceRepository spaceRepository;

    private String adminToken;
    private String userToken;

    private static final UUID EXISTING_SPACE_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID EXISTING_SPACE_ID_READ_ONLY = UUID.fromString("660e8400-e29b-41d4-a716-446655440005");
    private static final UUID NON_EXISTING_SPACE_ID = UUID.fromString("999e8400-e29b-41d4-a716-446655440999");

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin", "admin123");
        userToken = obtainToken("user", "user123");
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
    @DisplayName("GET /spaces")
    class GetAllSpaces {

        @Test
        @DisplayName("getAllSpaces_ReturnsAllSpaces_200")
        void getAllSpaces_ReturnsAllSpaces_200() throws Exception {
            mockMvc.perform(get("/api/v1/spaces")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").exists())
                    .andExpect(jsonPath("$[0].status").exists());
        }
    }

    @Nested
    @DisplayName("GET /spaces/{id}")
    class GetSpaceById {

        @Test
        @DisplayName("getSpaceById_ExistingId_Returns200")
        void getSpaceById_ExistingId_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/spaces/{id}", EXISTING_SPACE_ID_READ_ONLY)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(EXISTING_SPACE_ID_READ_ONLY.toString()))
                    .andExpect(jsonPath("$.name").value("Espacio Coworking"))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        @Test
        @DisplayName("getSpaceById_NonExistingId_Returns404")
        void getSpaceById_NonExistingId_Returns404() throws Exception {
            mockMvc.perform(get("/api/v1/spaces/{id}", NON_EXISTING_SPACE_ID)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("GET /spaces/filter")
    class FilterSpaces {

        @Test
        @DisplayName("filterSpaces_ByStatus_Returns200")
        void filterSpaces_ByStatus_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/spaces/filter")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("status", "AVAILABLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("filterSpaces_ByMinCapacity_Returns200")
        void filterSpaces_ByMinCapacity_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/spaces/filter")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("minCapacity", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("filterSpaces_ByLocation_Returns200")
        void filterSpaces_ByLocation_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/spaces/filter")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("location", "Piso 1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("POST /spaces")
    class CreateSpace {

        @Test
        @DisplayName("createSpace_WithAdminToken_Returns201")
        void createSpace_WithAdminToken_Returns201() throws Exception {
            SpaceCreateRequest request = new SpaceCreateRequest(
                    "New Test Space",
                    "A space for testing",
                    5,
                    "Test Floor",
                    new BigDecimal("75.00")
            );

            mockMvc.perform(post("/api/v1/spaces")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Test Space"))
                    .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        @Test
        @DisplayName("createSpace_WithUserToken_Returns403")
        void createSpace_WithUserToken_Returns403() throws Exception {
            SpaceCreateRequest request = new SpaceCreateRequest(
                    "New Test Space",
                    "A space for testing",
                    5,
                    "Test Floor",
                    new BigDecimal("75.00")
            );

            mockMvc.perform(post("/api/v1/spaces")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("createSpace_WithoutToken_Returns401")
        void createSpace_WithoutToken_Returns401() throws Exception {
            SpaceCreateRequest request = new SpaceCreateRequest(
                    "New Test Space",
                    "A space for testing",
                    5,
                    "Test Floor",
                    new BigDecimal("75.00")
            );

            mockMvc.perform(post("/api/v1/spaces")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("createSpace_InvalidData_Returns400")
        void createSpace_InvalidData_Returns400() throws Exception {
            SpaceCreateRequest request = new SpaceCreateRequest(
                    "",
                    "A space for testing",
                    0,
                    "",
                    new BigDecimal("-1.00")
            );

            mockMvc.perform(post("/api/v1/spaces")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors").exists());
        }
    }

    @Nested
    @DisplayName("PUT /spaces/{id}")
    class UpdateSpace {

        @Test
        @DisplayName("updateSpace_WithAdminToken_Returns200")
        void updateSpace_WithAdminToken_Returns200() throws Exception {
            SpaceUpdateRequest request = new SpaceUpdateRequest(
                    "Updated Space Name",
                    "Updated description",
                    15,
                    "New Floor",
                    new BigDecimal("100.00"),
                    SpaceStatus.MAINTENANCE
            );

            mockMvc.perform(put("/api/v1/spaces/{id}", EXISTING_SPACE_ID)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Space Name"));
        }

        @Test
        @DisplayName("updateSpace_WithUserToken_Returns403")
        void updateSpace_WithUserToken_Returns403() throws Exception {
            SpaceUpdateRequest request = new SpaceUpdateRequest(
                    "Updated Space Name",
                    "Updated description",
                    15,
                    "New Floor",
                    new BigDecimal("100.00"),
                    SpaceStatus.MAINTENANCE
            );

            mockMvc.perform(put("/api/v1/spaces/{id}", EXISTING_SPACE_ID)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("updateSpace_NonExistingId_Returns404")
        void updateSpace_NonExistingId_Returns404() throws Exception {
            SpaceUpdateRequest request = new SpaceUpdateRequest(
                    "Updated Space Name",
                    "Updated description",
                    15,
                    "New Floor",
                    new BigDecimal("100.00"),
                    SpaceStatus.MAINTENANCE
            );

            mockMvc.perform(put("/api/v1/spaces/{id}", NON_EXISTING_SPACE_ID)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /spaces/{id}")
    class DeleteSpace {

        private UUID spaceToDeleteId;

        @BeforeEach
        void setUpDelete() throws Exception {
            SpaceCreateRequest request = new SpaceCreateRequest(
                    "Space To Delete",
                    "This space will be deleted",
                    5,
                    "Delete Floor",
                    new BigDecimal("50.00")
            );
            MvcResult result = mockMvc.perform(post("/api/v1/spaces")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();
            String responseBody = result.getResponse().getContentAsString();
            spaceToDeleteId = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());
        }

        @Test
        @DisplayName("deleteSpace_WithAdminToken_Returns204")
        void deleteSpace_WithAdminToken_Returns204() throws Exception {
            mockMvc.perform(delete("/api/v1/spaces/{id}", spaceToDeleteId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deleteSpace_WithUserToken_Returns403")
        void deleteSpace_WithUserToken_Returns403() throws Exception {
            mockMvc.perform(delete("/api/v1/spaces/{id}", spaceToDeleteId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deleteSpace_WithoutToken_Returns401")
        void deleteSpace_WithoutToken_Returns401() throws Exception {
            mockMvc.perform(delete("/api/v1/spaces/{id}", EXISTING_SPACE_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
