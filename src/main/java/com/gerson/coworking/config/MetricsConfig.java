package com.gerson.coworking.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MetricsConfig {

    private final AtomicLong activeReservations = new AtomicLong(0);

    @Bean
    public Counter reservationsCreatedCounter(MeterRegistry registry) {
        return Counter.builder("coworking.reservations.created")
                .description("Total number of reservations created")
                .register(registry);
    }

    @Bean
    public Counter reservationsConfirmedCounter(MeterRegistry registry) {
        return Counter.builder("coworking.reservations.confirmed")
                .description("Total number of reservations confirmed")
                .register(registry);
    }

    @Bean
    public Counter reservationsCancelledCounter(MeterRegistry registry) {
        return Counter.builder("coworking.reservations.cancelled")
                .description("Total number of reservations cancelled")
                .register(registry);
    }

    @Bean
    public Timer reservationCreationTimer(MeterRegistry registry) {
        return Timer.builder("coworking.reservation.create.time")
                .description("Time taken to create a reservation")
                .register(registry);
    }

    @Bean
    public Timer reservationConfirmationTimer(MeterRegistry registry) {
        return Timer.builder("coworking.reservation.confirm.time")
                .description("Time taken to confirm a reservation")
                .register(registry);
    }

    @Bean
    public AtomicLong activeReservationsGaugeValue() {
        return activeReservations;
    }

    @Bean
    public Gauge activeReservationsGauge(MeterRegistry registry, AtomicLong activeReservationsGaugeValue) {
        return Gauge.builder("coworking.reservations.active", activeReservationsGaugeValue, AtomicLong::get)
                .description("Current number of active (confirmed) reservations")
                .register(registry);
    }
}