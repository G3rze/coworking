package com.gerson.coworking.controller;

import com.gerson.coworking.domain.dto.space.SpaceCreateRequest;
import com.gerson.coworking.domain.dto.space.SpaceResponse;
import com.gerson.coworking.domain.dto.space.SpaceUpdateRequest;
import com.gerson.coworking.domain.enums.SpaceStatus;
import com.gerson.coworking.service.SpaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    public ResponseEntity<List<SpaceResponse>> getAllSpaces() {
        return ResponseEntity.ok(spaceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaceResponse> getSpaceById(@PathVariable UUID id) {
        return spaceService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new com.gerson.coworking.exception.ResourceNotFoundException("Space", "id", id));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<SpaceResponse>> filterSpaces(
            @RequestParam(required = false) SpaceStatus status,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(spaceService.filter(status, minCapacity, location));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpaceResponse> createSpace(@Valid @RequestBody SpaceCreateRequest request) {
        SpaceResponse response = spaceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpaceResponse> updateSpace(
            @PathVariable UUID id,
            @Valid @RequestBody SpaceUpdateRequest request) {
        return ResponseEntity.ok(spaceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSpace(@PathVariable UUID id) {
        spaceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
