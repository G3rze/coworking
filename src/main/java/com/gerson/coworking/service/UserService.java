package com.gerson.coworking.service;

import com.gerson.coworking.domain.dto.user.UserCreateRequest;
import com.gerson.coworking.domain.dto.user.UserResponse;
import com.gerson.coworking.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createUser(UserCreateRequest request);

    List<UserResponse> findAll();

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
