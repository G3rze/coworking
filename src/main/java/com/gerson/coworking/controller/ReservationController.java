package com.gerson.coworking.controller;

import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationFilterRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;
import com.gerson.coworking.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(reservationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable UUID id) {
        return reservationService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new com.gerson.coworking.exception.ResourceNotFoundException("Reservation", "id", id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(reservationService.findByUser(userId));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ReservationResponse>> filterReservations(
            @RequestParam(required = false) UUID spaceId,
            @RequestParam(required = false) java.time.LocalDate dateFrom,
            @RequestParam(required = false) java.time.LocalDate dateTo,
            @RequestParam(required = false) com.gerson.coworking.domain.enums.ReservationStatus status) {
        ReservationFilterRequest filter = new ReservationFilterRequest(spaceId, dateFrom, dateTo, status);
        return ResponseEntity.ok(reservationService.filter(filter));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestParam UUID userId,
            @Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse response = reservationService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }
}
