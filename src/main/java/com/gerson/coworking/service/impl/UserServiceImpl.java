package com.gerson.coworking.service.impl;

import com.gerson.coworking.config.AppProperties;
import com.gerson.coworking.config.ZoneIdProvider;
import com.gerson.coworking.domain.dto.auth.LoginResponse;
import com.gerson.coworking.domain.dto.auth.UserInfo;
import com.gerson.coworking.domain.dto.user.UserCreateRequest;
import com.gerson.coworking.domain.dto.user.UserResponse;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.domain.mapper.UserMapper;
import com.gerson.coworking.repository.UserRepository;
import com.gerson.coworking.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ZoneIdProvider zoneIdProvider,
                           JwtTokenProvider jwtTokenProvider,
                           AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.zoneIdProvider = zoneIdProvider;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appProperties = appProperties;
    }

    @Override
    public LoginResponse authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId());

        UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), user.getRole());

        return new LoginResponse(token, "Bearer", appProperties.getJwt().getExpiration(), userInfo);
    }

    @Override
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        validatePassword(request.password());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return userRepository.save(user);
    }

    private void validatePassword(String password) {
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!password.matches(pattern)) {
            throw new IllegalArgumentException(
                    "Password must contain at least 1 uppercase, 1 lowercase, 1 digit, and 1 special character");
        }
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