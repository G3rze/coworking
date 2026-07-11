package com.gerson.coworking.service;

import com.gerson.coworking.domain.dto.space.SpaceCreateRequest;
import com.gerson.coworking.domain.dto.space.SpaceResponse;
import com.gerson.coworking.domain.dto.space.SpaceUpdateRequest;
import com.gerson.coworking.domain.enums.SpaceStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceService {

    SpaceResponse create(SpaceCreateRequest request);

    SpaceResponse update(UUID id, SpaceUpdateRequest request);

    void delete(UUID id);

    List<SpaceResponse> findAll();

    Optional<SpaceResponse> findById(UUID id);

    List<SpaceResponse> filter(SpaceStatus status, Integer minCapacity, String location);
}
