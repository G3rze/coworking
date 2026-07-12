package com.gerson.coworking.domain.dto.reservation;

import java.util.UUID;

public record UserBasicInfo(
        UUID id,
        String username,
        String email
) {}
