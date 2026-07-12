package com.gerson.coworking.domain.dto.reservation;

import java.util.UUID;

public record SpaceBasicInfo(
        UUID id,
        String name,
        String location
) {}
