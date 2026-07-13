package com.gerson.coworking.service.impl;

import com.gerson.coworking.config.ZoneIdProvider;
import com.gerson.coworking.domain.dto.space.SpaceCreateRequest;
import com.gerson.coworking.domain.dto.space.SpaceResponse;
import com.gerson.coworking.domain.dto.space.SpaceUpdateRequest;
import com.gerson.coworking.domain.entity.Space;
import com.gerson.coworking.domain.enums.SpaceStatus;
import com.gerson.coworking.domain.mapper.SpaceMapper;
import com.gerson.coworking.exception.ResourceNotFoundException;
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
    private final ZoneIdProvider zoneIdProvider;

    public SpaceServiceImpl(SpaceRepository spaceRepository, ZoneIdProvider zoneIdProvider) {
        this.spaceRepository = spaceRepository;
        this.zoneIdProvider = zoneIdProvider;
    }

    @Override
    public SpaceResponse create(SpaceCreateRequest request) {
        Space space = Space.builder()
                .name(request.name())
                .description(request.description())
                .capacity(request.capacity())
                .location(request.location())
                .pricePerHour(request.pricePerHour())
                .status(SpaceStatus.AVAILABLE)
                .build();

        Space saved = spaceRepository.save(space);
        return SpaceMapper.toResponse(saved, zoneIdProvider.getZoneId());
    }

    @Override
    public SpaceResponse update(UUID id, SpaceUpdateRequest request) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space", "id", id));

        space.setName(request.name());
        space.setDescription(request.description());
        space.setCapacity(request.capacity());
        space.setLocation(request.location());
        space.setPricePerHour(request.pricePerHour());
        space.setStatus(request.status());

        Space updated = spaceRepository.save(space);
        return SpaceMapper.toResponse(updated, zoneIdProvider.getZoneId());
    }

    @Override
    public void delete(UUID id) {
        if (!spaceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Space", "id", id);
        }
        spaceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceResponse> findAll() {
        return spaceRepository.findAll().stream()
                .map(s -> SpaceMapper.toResponse(s, zoneIdProvider.getZoneId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpaceResponse> findById(UUID id) {
        return spaceRepository.findById(id)
                .map(s -> SpaceMapper.toResponse(s, zoneIdProvider.getZoneId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceResponse> filter(SpaceStatus status, Integer minCapacity, String location) {
        List<Space> spaces = spaceRepository.findAll();

        return spaces.stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> minCapacity == null || s.getCapacity() >= minCapacity)
                .filter(s -> location == null || s.getLocation().toLowerCase().contains(location.toLowerCase()))
                .map(s -> SpaceMapper.toResponse(s, zoneIdProvider.getZoneId()))
                .collect(Collectors.toList());
    }
}
