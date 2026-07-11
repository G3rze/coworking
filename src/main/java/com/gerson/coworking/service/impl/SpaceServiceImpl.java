package com.gerson.coworking.service.impl;

import com.gerson.coworking.domain.dto.space.SpaceCreateRequest;
import com.gerson.coworking.domain.dto.space.SpaceResponse;
import com.gerson.coworking.domain.dto.space.SpaceUpdateRequest;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.domain.enums.SpaceStatus;
import com.gerson.coworking.repository.SpaceRepository;
import com.gerson.coworking.service.SpaceService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpaceServiceImpl implements SpaceService {

    private final SpaceRepository spaceRepository;

    public SpaceServiceImpl(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    @Override
    public SpaceResponse create(SpaceCreateRequest request) {
        Space space = Space.builder()
                .name(request.getName())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .location(request.getLocation())
                .pricePerHour(request.getPricePerHour())
                .status(SpaceStatus.AVAILABLE)
                .build();

        Space saved = spaceRepository.save(space);
        return mapToResponse(saved);
    }

    @Override
    public SpaceResponse update(UUID id, SpaceUpdateRequest request) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + id));

        space.setName(request.getName());
        space.setDescription(request.getDescription());
        space.setCapacity(request.getCapacity());
        space.setLocation(request.getLocation());
        space.setPricePerHour(request.getPricePerHour());
        space.setStatus(request.getStatus());

        Space updated = spaceRepository.save(space);
        return mapToResponse(updated);
    }

    @Override
    public void delete(UUID id) {
        if (!spaceRepository.existsById(id)) {
            throw new RuntimeException("Space not found with id: " + id);
        }
        spaceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceResponse> findAll() {
        return spaceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpaceResponse> findById(UUID id) {
        return spaceRepository.findById(id)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceResponse> filter(SpaceStatus status, Integer minCapacity, String location) {
        List<Space> spaces = spaceRepository.findAll();

        return spaces.stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> minCapacity == null || s.getCapacity() >= minCapacity)
                .filter(s -> location == null || s.getLocation().toLowerCase().contains(location.toLowerCase()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SpaceResponse mapToResponse(Space space) {
        return SpaceResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .description(space.getDescription())
                .capacity(space.getCapacity())
                .location(space.getLocation())
                .pricePerHour(space.getPricePerHour())
                .status(space.getStatus())
                .createdAt(space.getCreatedAt())
                .build();
    }
}
