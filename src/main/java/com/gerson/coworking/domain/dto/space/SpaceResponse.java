package com.gerson.coworking.domain.dto.space;

import com.gerson.coworking.domain.enums.SpaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceResponse {

    private UUID id;
    private String name;
    private String description;
    private Integer capacity;
    private String location;
    private BigDecimal pricePerHour;
    private SpaceStatus status;
    private Instant createdAt;
}
