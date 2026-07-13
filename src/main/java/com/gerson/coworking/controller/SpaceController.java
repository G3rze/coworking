package com.gerson.coworking.controller;

import com.gerson.coworking.domain.dto.space.SpaceCreateRequest;
import com.gerson.coworking.domain.dto.space.SpaceResponse;
import com.gerson.coworking.domain.dto.space.SpaceUpdateRequest;
import com.gerson.coworking.domain.enums.SpaceStatus;
import com.gerson.coworking.exception.ErrorResponse;
import com.gerson.coworking.exception.ResourceNotFoundException;
import com.gerson.coworking.service.SpaceService;
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
@RequestMapping("/api/v1/spaces")
@Tag(name = "Spaces", description = "Coworking space management operations")
@SecurityRequirement(name = "bearerAuth")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @Operation(summary = "Get all spaces", description = "Retrieves a list of all available coworking spaces")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Spaces retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<SpaceResponse>> getAllSpaces() {
        return ResponseEntity.ok(spaceService.findAll());
    }

    @Operation(summary = "Get space by ID", description = "Retrieves a specific coworking space by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Space found",
                    content = @Content(schema = @Schema(implementation = SpaceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Space not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SpaceResponse> getSpaceById(
            @Parameter(description = "Space unique identifier") @PathVariable UUID id) {
        return spaceService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Space", "id", id));
    }

    @Operation(summary = "Filter spaces", description = "Search and filter spaces by status, minimum capacity, and/or location")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filter results returned successfully")
    })
    @GetMapping("/filter")
    public ResponseEntity<List<SpaceResponse>> filterSpaces(
            @Parameter(description = "Filter by availability status") @RequestParam(required = false) SpaceStatus status,
            @Parameter(description = "Minimum capacity requirement") @RequestParam(required = false) Integer minCapacity,
            @Parameter(description = "Location search keyword") @RequestParam(required = false) String location) {
        return ResponseEntity.ok(spaceService.filter(status, minCapacity, location));
    }

    @Operation(summary = "Create new space", description = "Creates a new coworking space. Only administrators can perform this operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Space created successfully",
                    content = @Content(schema = @Schema(implementation = SpaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpaceResponse> createSpace(@Valid @RequestBody SpaceCreateRequest request) {
        SpaceResponse response = spaceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update space", description = "Updates an existing coworking space. Only administrators can perform this operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Space updated successfully",
                    content = @Content(schema = @Schema(implementation = SpaceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Space not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpaceResponse> updateSpace(
            @Parameter(description = "Space unique identifier") @PathVariable UUID id,
            @Valid @RequestBody SpaceUpdateRequest request) {
        return ResponseEntity.ok(spaceService.update(id, request));
    }

    @Operation(summary = "Delete space", description = "Deletes a coworking space. Only administrators can perform this operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Space deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Space not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSpace(
            @Parameter(description = "Space unique identifier") @PathVariable UUID id) {
        spaceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
