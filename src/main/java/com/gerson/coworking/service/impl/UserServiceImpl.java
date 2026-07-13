package com.gerson.coworking.service.impl;

import com.gerson.coworking.config.ZoneIdProvider;
import com.gerson.coworking.domain.dto.user.UserCreateRequest;
import com.gerson.coworking.domain.dto.user.UserResponse;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.domain.mapper.UserMapper;
import com.gerson.coworking.repository.UserRepository;
import com.gerson.coworking.service.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ZoneIdProvider zoneIdProvider;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, ZoneIdProvider zoneIdProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.zoneIdProvider = zoneIdProvider;
    }

    @Override
    public User createUser(UserCreateRequest request) {
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(u -> UserMapper.toResponse(u, zoneIdProvider.getZoneId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
