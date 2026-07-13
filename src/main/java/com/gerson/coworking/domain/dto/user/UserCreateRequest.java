package com.gerson.coworking.domain.dto.user;

import com.gerson.coworking.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "User creation request")
public record UserCreateRequest(
        @Schema(description = "Unique username (3-50 characters)", example = "newuser")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Schema(description = "Valid email address", example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @Schema(description = "Password (min 8 chars with uppercase, lowercase, digit, special char)", example = "Password123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(description = "User role", example = "USER")
        @NotNull(message = "Role is required")
        Role role
) {}