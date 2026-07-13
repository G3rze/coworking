package com.gerson.coworking.service;

import com.gerson.coworking.config.ZoneIdProvider;
import com.gerson.coworking.domain.dto.space.SpaceCreateRequest;
import com.gerson.coworking.domain.dto.space.SpaceResponse;
import com.gerson.coworking.domain.dto.space.SpaceUpdateRequest;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.domain.enums.SpaceStatus;
import com.gerson.coworking.domain.mapper.SpaceMapper;
import com.gerson.coworking.exception.ResourceNotFoundException;
import com.gerson.coworking.repository.SpaceRepository;
import com.gerson.coworking.service.impl.SpaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpaceServiceImplTest {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private ZoneIdProvider zoneIdProvider;

    private SpaceServiceImpl spaceService;

    private Space testSpace;
    private SpaceResponse testSpaceResponse;
    private final UUID testSpaceId = UUID.randomUUID();
    private final ZoneId testZone = ZoneId.of("America/El_Salvador");

    @BeforeEach
    void setUp() {
        spaceService = new SpaceServiceImpl(spaceRepository, zoneIdProvider);

        testSpace = Space.builder()
                .id(testSpaceId)
                .name("Conference Room A")
                .description("Large meeting room")
                .capacity(10)
                .location("Floor 2")
                .pricePerHour(new BigDecimal("25.00"))
                .status(SpaceStatus.AVAILABLE)
                .build();
        testSpace.setCreatedAt(java.time.Instant.now());

        testSpaceResponse = SpaceMapper.toResponse(testSpace, testZone);
    }

    @Nested
    @DisplayName("create_Success scenarios")
    class CreateSuccess {

        @Test
        @DisplayName("create_Success_ReturnsCreatedSpace")
        void create_Success_ReturnsCreatedSpace() {
            SpaceCreateRequest request = new SpaceCreateRequest(
                    "Conference Room A",
                    "Large meeting room",
                    10,
                    "Floor 2",
                    new BigDecimal("25.00")
            );

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.save(any(Space.class))).thenReturn(testSpace);

            SpaceResponse result = spaceService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Conference Room A");
            assertThat(result.capacity()).isEqualTo(10);
            assertThat(result.location()).isEqualTo("Floor 2");
            verify(spaceRepository, times(1)).save(any(Space.class));
        }

        @Test
        @DisplayName("create_SetsAvailableStatus_OnNewSpace")
        void create_SetsAvailableStatus_OnNewSpace() {
            SpaceCreateRequest request = new SpaceCreateRequest(
                    "New Space",
                    "Description",
                    5,
                    "Floor 1",
                    new BigDecimal("15.00")
            );

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            ArgumentCaptor<Space> spaceCaptor = ArgumentCaptor.forClass(Space.class);
            when(spaceRepository.save(spaceCaptor.capture())).thenReturn(testSpace);

            spaceService.create(request);

            Space capturedSpace = spaceCaptor.getValue();
            assertThat(capturedSpace.getStatus()).isEqualTo(SpaceStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("create_Failure scenarios")
    class CreateFailure {
        // No specific failure scenarios for create since all validations are in DTO
    }

    @Nested
    @DisplayName("update_Success scenarios")
    class UpdateSuccess {

        @Test
        @DisplayName("update_Success_ReturnsUpdatedSpace")
        void update_Success_ReturnsUpdatedSpace() {
            SpaceUpdateRequest request = new SpaceUpdateRequest(
                    "Updated Room",
                    "Updated description",
                    15,
                    "Floor 3",
                    new BigDecimal("35.00"),
                    SpaceStatus.MAINTENANCE
            );

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findById(testSpaceId)).thenReturn(Optional.of(testSpace));
            when(spaceRepository.save(any(Space.class))).thenReturn(testSpace);

            SpaceResponse result = spaceService.update(testSpaceId, request);

            assertThat(result).isNotNull();
            verify(spaceRepository, times(1)).findById(testSpaceId);
            verify(spaceRepository, times(1)).save(any(Space.class));
        }
    }

    @Nested
    @DisplayName("update_Failure scenarios")
    class UpdateFailure {

        @Test
        @DisplayName("update_NotFound_ThrowsResourceNotFoundException")
        void update_NotFound_ThrowsResourceNotFoundException() {
            SpaceUpdateRequest request = new SpaceUpdateRequest(
                    "Updated Room",
                    "Updated description",
                    15,
                    "Floor 3",
                    new BigDecimal("35.00"),
                    SpaceStatus.MAINTENANCE
            );

            UUID nonExistentId = UUID.randomUUID();
            when(spaceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> spaceService.update(nonExistentId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Space")
                    .hasMessageContaining(nonExistentId.toString());
        }
    }

    @Nested
    @DisplayName("delete_Success scenarios")
    class DeleteSuccess {

        @Test
        @DisplayName("delete_Success_DeletesExistingSpace")
        void delete_Success_DeletesExistingSpace() {
            when(spaceRepository.existsById(testSpaceId)).thenReturn(true);
            doNothing().when(spaceRepository).deleteById(testSpaceId);

            spaceService.delete(testSpaceId);

            verify(spaceRepository, times(1)).deleteById(testSpaceId);
        }
    }

    @Nested
    @DisplayName("delete_Failure scenarios")
    class DeleteFailure {

        @Test
        @DisplayName("delete_NotFound_ThrowsResourceNotFoundException")
        void delete_NotFound_ThrowsResourceNotFoundException() {
            UUID nonExistentId = UUID.randomUUID();
            when(spaceRepository.existsById(nonExistentId)).thenReturn(false);

            assertThatThrownBy(() -> spaceService.delete(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Space");
        }
    }

    @Nested
    @DisplayName("findAll_Success scenarios")
    class FindAllSuccess {

        @Test
        @DisplayName("findAll_ReturnsAllSpaces")
        void findAll_ReturnsAllSpaces() {
            Space secondSpace = Space.builder()
                    .id(UUID.randomUUID())
                    .name("Private Office")
                    .description("Small office")
                    .capacity(2)
                    .location("Floor 1")
                    .pricePerHour(new BigDecimal("50.00"))
                    .status(SpaceStatus.AVAILABLE)
                    .build();
            secondSpace.setCreatedAt(java.time.Instant.now());

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findAll()).thenReturn(List.of(testSpace, secondSpace));

            List<SpaceResponse> result = spaceService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Conference Room A");
            assertThat(result.get(1).name()).isEqualTo("Private Office");
        }

        @Test
        @DisplayName("findAll_EmptyDatabase_ReturnsEmptyList")
        void findAll_EmptyDatabase_ReturnsEmptyList() {
            when(spaceRepository.findAll()).thenReturn(List.of());

            List<SpaceResponse> result = spaceService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById_Success scenarios")
    class FindByIdSuccess {

        @Test
        @DisplayName("findById_ExistingId_ReturnsSpace")
        void findById_ExistingId_ReturnsSpace() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findById(testSpaceId)).thenReturn(Optional.of(testSpace));

            Optional<SpaceResponse> result = spaceService.findById(testSpaceId);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(testSpaceId);
        }
    }

    @Nested
    @DisplayName("findById_Failure scenarios")
    class FindByIdFailure {

        @Test
        @DisplayName("findById_NonExistingId_ReturnsEmpty")
        void findById_NonExistingId_ReturnsEmpty() {
            UUID nonExistentId = UUID.randomUUID();
            when(spaceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            Optional<SpaceResponse> result = spaceService.findById(nonExistentId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("filter_Success scenarios")
    class FilterSuccess {

        @Test
        @DisplayName("filter_ByStatus_ReturnsMatchingSpaces")
        void filter_ByStatus_ReturnsMatchingSpaces() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findAll()).thenReturn(List.of(testSpace));

            List<SpaceResponse> result = spaceService.filter(SpaceStatus.AVAILABLE, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(SpaceStatus.AVAILABLE);
        }

        @Test
        @DisplayName("filter_ByMinCapacity_ReturnsSpacesWithSufficientCapacity")
        void filter_ByMinCapacity_ReturnsSpacesWithSufficientCapacity() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findAll()).thenReturn(List.of(testSpace));

            List<SpaceResponse> result = spaceService.filter(null, 5, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).capacity()).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("filter_ByMinCapacity_ExcludesSpacesWithInsufficientCapacity")
        void filter_ByMinCapacity_ExcludesSpacesWithInsufficientCapacity() {
            when(spaceRepository.findAll()).thenReturn(List.of(testSpace));

            List<SpaceResponse> result = spaceService.filter(null, 15, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("filter_ByLocation_ReturnsMatchingSpaces")
        void filter_ByLocation_ReturnsMatchingSpaces() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findAll()).thenReturn(List.of(testSpace));

            List<SpaceResponse> result = spaceService.filter(null, null, "Floor 2");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).location()).contains("Floor 2");
        }

        @Test
        @DisplayName("filter_ByLocation_CaseInsensitive")
        void filter_ByLocation_CaseInsensitive() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findAll()).thenReturn(List.of(testSpace));

            List<SpaceResponse> result = spaceService.filter(null, null, "floor 2");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("filter_CombinedCriteria_ReturnsMatchingSpaces")
        void filter_CombinedCriteria_ReturnsMatchingSpaces() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findAll()).thenReturn(List.of(testSpace));

            List<SpaceResponse> result = spaceService.filter(SpaceStatus.AVAILABLE, 5, "Floor");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("filter_AllNullParams_ReturnsAllSpaces")
        void filter_AllNullParams_ReturnsAllSpaces() {
            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(spaceRepository.findAll()).thenReturn(List.of(testSpace));

            List<SpaceResponse> result = spaceService.filter(null, null, null);

            assertThat(result).hasSize(1);
        }
    }
}
