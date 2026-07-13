package com.gerson.coworking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class ZoneIdProvider {

    private final AppProperties appProperties;

    public ZoneId getZoneId() {
        return ZoneId.of(appProperties.getTimezone());
    }
}