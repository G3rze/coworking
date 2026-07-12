package com.gerson.coworking.domain.dto.auth;

import com.gerson.coworking.domain.enums.Role;

import java.util.UUID;

public record UserInfo(
        UUID id,
        String username,
        Role role
) {}
