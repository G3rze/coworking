package com.gerson.coworking.controller;

import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationFilterRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;
import com.gerson.coworking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservations")
@Tag(name = "Reservations", description = "Reservation management operations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Get all reservations", description = "Retrieves all reservations. Only administrators can access this endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(reservationService.findAll());
    }

    @Operation(summary = "Get reservation by ID", description = "Retrieves a specific reservation by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation found",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(
            @Parameter(description = "Reservation unique identifier") @PathVariable UUID id) {
        return reservationService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new com.gerson.coworking.exception.ResourceNotFoundException("Reservation", "id", id));
    }

    @Operation(summary = "Get reservations by user", description = "Retrieves all reservations for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User reservations retrieved successfully")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUser(
            @Parameter(description = "User unique identifier") @PathVariable UUID userId) {
        return ResponseEntity.ok(reservationService.findByUser(userId));
    }

    @Operation(summary = "Filter reservations", description = "Search and filter reservations by space, date range, and/or status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filter results returned successfully")
    })
    @GetMapping("/filter")
    public ResponseEntity<List<ReservationResponse>> filterReservations(
            @Parameter(description = "Filter by space ID") @RequestParam(required = false) UUID spaceId,
            @Parameter(description = "Filter from date (inclusive)") @RequestParam(required = false) java.time.LocalDate dateFrom,
            @Parameter(description = "Filter to date (inclusive)") @RequestParam(required = false) java.time.LocalDate dateTo,
            @Parameter(description = "Filter by status") @RequestParam(required = false) com.gerson.coworking.domain.enums.ReservationStatus status) {
        ReservationFilterRequest filter = new ReservationFilterRequest(spaceId, dateFrom, dateTo, status);
        return ResponseEntity.ok(reservationService.filter(filter));
    }

    @Operation(summary = "Create reservation", description = "Creates a new space reservation. Requires user authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reservation created successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Space or user not found",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Time slot conflict - overlapping reservation exists",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Parameter(description = "User ID making the reservation") @RequestParam UUID userId,
            @Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse response = reservationService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Confirm reservation", description = "Confirms a pending reservation after payment validation. Only works on reservations in PENDING_PAYMENT status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation confirmed successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cannot confirm - reservation is not in PENDING_PAYMENT status",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class)))
    })
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @Parameter(description = "Reservation unique identifier") @PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }

    @Operation(summary = "Cancel reservation", description = "Cancels an existing reservation. Can be performed on PENDING_PAYMENT or CONFIRMED reservations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation cancelled successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cannot cancel - reservation is already cancelled",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @Parameter(description = "Reservation unique identifier") @PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }
}
