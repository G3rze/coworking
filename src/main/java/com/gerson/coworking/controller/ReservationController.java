package com.gerson.coworking.controller;

import com.gerson.coworking.domain.dto.reservation.ReservationCreateRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationFilterRequest;
import com.gerson.coworking.domain.dto.reservation.ReservationResponse;
import com.gerson.coworking.domain.enums.ReservationStatus;
import com.gerson.coworking.exception.ErrorResponse;
import com.gerson.coworking.security.JwtTokenProvider;
import com.gerson.coworking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Reservation management operations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;
    private final JwtTokenProvider jwtTokenProvider;

    public ReservationController(ReservationService reservationService, JwtTokenProvider jwtTokenProvider) {
        this.reservationService = reservationService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Operation(summary = "Get all reservations", description = "Retrieves all reservations. Only administrators can access this endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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
            @ApiResponse(responseCode = "403", description = "Access denied - reservation belongs to another user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(
            @Parameter(description = "Reservation unique identifier") @PathVariable UUID id,
            HttpServletRequest request) {
        return ResponseEntity.ok(reservationService.findByIdForUser(
                id, getCurrentUserId(request), isAdmin(request)));
    }

    @Operation(summary = "Get reservations by user", description = "Retrieves all reservations for a specific user. Only administrators can query other users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User reservations retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - users can only view their own reservations",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUser(
            @Parameter(description = "User unique identifier") @PathVariable UUID userId,
            HttpServletRequest request) {
        return ResponseEntity.ok(reservationService.findByUserForUser(
                userId, getCurrentUserId(request), isAdmin(request)));
    }

    @Operation(summary = "Filter reservations", description = "Search and filter reservations by space, date range, and/or status. Users see only their own reservations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filter results returned successfully")
    })
    @GetMapping("/filter")
    public ResponseEntity<List<ReservationResponse>> filterReservations(
            @Parameter(description = "Filter by space ID") @RequestParam(required = false) UUID spaceId,
            @Parameter(description = "Filter from date (inclusive)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter to date (inclusive)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by status") @RequestParam(required = false) ReservationStatus status,
            HttpServletRequest request) {
        ReservationFilterRequest filter = new ReservationFilterRequest(spaceId, dateFrom, dateTo, status);
        return ResponseEntity.ok(reservationService.filterForUser(
                filter, getCurrentUserId(request), isAdmin(request)));
    }

    @Operation(summary = "Create reservation", description = "Creates a new space reservation. Users create for themselves. Admins create for a specific user (required).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reservation created successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or business rule violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot specify userId for reservation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Space or user not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Time slot conflict - overlapping reservation exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Parameter(description = "Target user ID (required for admin, forbidden for user)") @RequestParam(required = false) UUID userId,
            @Valid @RequestBody ReservationCreateRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                reservationService.createReservation(
                        getCurrentUserId(httpRequest), isAdmin(httpRequest), userId, request));
    }

    @Operation(summary = "Confirm reservation", description = "Confirms a pending reservation after payment validation. Only works on reservations in PENDING_PAYMENT status. Only admins can confirm.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation confirmed successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cannot confirm - reservation is not in PENDING_PAYMENT status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @Parameter(description = "Reservation unique identifier") @PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }

    @Operation(summary = "Cancel reservation", description = "Cancels an existing reservation. Users can only cancel their own reservations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation cancelled successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cannot cancel - reservation is already cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - reservation belongs to another user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @Parameter(description = "Reservation unique identifier") @PathVariable UUID id,
            HttpServletRequest request) {
        return ResponseEntity.ok(reservationService.cancelForUser(
                id, getCurrentUserId(request), isAdmin(request)));
    }

    @Operation(summary = "Complete reservation", description = "Marks a confirmed reservation as completed. Only available after the reservation end time has passed. Only admins can perform this operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation completed successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cannot complete - reservation not confirmed or still in progress",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> completeReservation(
            @Parameter(description = "Reservation unique identifier") @PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.complete(id));
    }

    private UUID getCurrentUserId(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    private boolean isAdmin(HttpServletRequest request) {
        Authentication authentication = (Authentication) request.getAttribute("org.springframework.security.web.csrf.CsrfAuthenticationEntryPoint");
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new AccessDeniedException("Authentication required");
        }
        return bearerToken.substring(7);
    }
}