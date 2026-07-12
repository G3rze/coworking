package com.gerson.coworking.domain.dto.user;

import com.gerson.coworking.domain.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Role role,
        Instant createdAt
) {}
