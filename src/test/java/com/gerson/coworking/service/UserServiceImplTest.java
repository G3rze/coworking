package com.gerson.coworking.service;

import com.gerson.coworking.config.ZoneIdProvider;
import com.gerson.coworking.domain.dto.user.UserCreateRequest;
import com.gerson.coworking.domain.dto.user.UserResponse;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.domain.enums.Role;
import com.gerson.coworking.domain.mapper.UserMapper;
import com.gerson.coworking.repository.UserRepository;
import com.gerson.coworking.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ZoneIdProvider zoneIdProvider;

    private UserServiceImpl userService;

    private User testUser;
    private final UUID testUserId = UUID.randomUUID();
    private final ZoneId testZone = ZoneId.of("America/El_Salvador");

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder, zoneIdProvider);

        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
                .role(Role.USER)
                .build();
        testUser.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("createUser_Success scenarios")
    class CreateUserSuccess {

        @Test
        @DisplayName("createUser_Success_EncodesPassword")
        void createUser_Success_EncodesPassword() {
            UserCreateRequest request = new UserCreateRequest(
                    "newuser",
                    "newuser@example.com",
                    "Password123!",
                    Role.USER
            );

            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");

            userService.createUser(request);

            verify(passwordEncoder, times(1)).encode("Password123!");
        }

        @Test
        @DisplayName("createUser_Success_SetsRole")
        void createUser_Success_SetsRole() {
            UserCreateRequest request = new UserCreateRequest(
                    "newuser",
                    "newuser@example.com",
                    "Password123!",
                    Role.ADMIN
            );

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenReturn(testUser);
            when(passwordEncoder.encode(any())).thenReturn("encoded_password");

            userService.createUser(request);

            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("createUser_Success_ReturnsSavedUser")
        void createUser_Success_ReturnsSavedUser() {
            UserCreateRequest request = new UserCreateRequest(
                    "newuser",
                    "newuser@example.com",
                    "Password123!",
                    Role.USER
            );

            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(passwordEncoder.encode(any())).thenReturn("encoded_password");

            User result = userService.createUser(request);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("findAll_Success scenarios")
    class FindAllSuccess {

        @Test
        @DisplayName("findAll_ReturnsAllUsers")
        void findAll_ReturnsAllUsers() {
            User secondUser = User.builder()
                    .id(UUID.randomUUID())
                    .username("anotheruser")
                    .email("another@example.com")
                    .password("encoded")
                    .role(Role.USER)
                    .build();
            secondUser.setCreatedAt(Instant.now());

            when(zoneIdProvider.getZoneId()).thenReturn(testZone);
            when(userRepository.findAll()).thenReturn(List.of(testUser, secondUser));

            List<UserResponse> result = userService.findAll();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("findAll_EmptyDatabase_ReturnsEmptyList")
        void findAll_EmptyDatabase_ReturnsEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserResponse> result = userService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUsername_Success scenarios")
    class FindByUsernameSuccess {

        @Test
        @DisplayName("findByUsername_ExistingUser_ReturnsUser")
        void findByUsername_ExistingUser_ReturnsUser() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            Optional<User> result = userService.findByUsername("testuser");

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("findByUsername_NonExistingUser_ReturnsEmpty")
        void findByUsername_NonExistingUser_ReturnsEmpty() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            Optional<User> result = userService.findByUsername("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUsername_Success scenarios")
    class ExistsByUsernameSuccess {

        @Test
        @DisplayName("existsByUsername_ExistingUser_ReturnsTrue")
        void existsByUsername_ExistingUser_ReturnsTrue() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            boolean result = userService.existsByUsername("testuser");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("existsByUsername_NonExistingUser_ReturnsFalse")
        void existsByUsername_NonExistingUser_ReturnsFalse() {
            when(userRepository.existsByUsername("nonexistent")).thenReturn(false);

            boolean result = userService.existsByUsername("nonexistent");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByEmail_Success scenarios")
    class ExistsByEmailSuccess {

        @Test
        @DisplayName("existsByEmail_ExistingEmail_ReturnsTrue")
        void existsByEmail_ExistingEmail_ReturnsTrue() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            boolean result = userService.existsByEmail("test@example.com");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("existsByEmail_NonExistingEmail_ReturnsFalse")
        void existsByEmail_NonExistingEmail_ReturnsFalse() {
            when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

            boolean result = userService.existsByEmail("nonexistent@example.com");

            assertThat(result).isFalse();
        }
    }
}
