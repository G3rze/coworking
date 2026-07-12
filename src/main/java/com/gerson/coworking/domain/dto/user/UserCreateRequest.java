package com.gerson.coworking.domain.dto.user;

import com.gerson.coworking.domain.enums.Role;
import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotNull(message = "Role is required")
        Role role
) {
    public UserCreateRequest {
        if (password != null && password.length() > 0) {
            String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
            if (!password.matches(pattern)) {
                throw new IllegalArgumentException(
                        "Password must contain at least 1 uppercase, 1 lowercase, 1 digit, and 1 special character");
            }
        }
    }
}
