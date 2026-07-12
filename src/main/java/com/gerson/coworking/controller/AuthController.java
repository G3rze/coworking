package com.gerson.coworking.controller;

import com.gerson.coworking.config.JwtProperties;
import com.gerson.coworking.domain.dto.auth.LoginRequest;
import com.gerson.coworking.domain.dto.auth.LoginResponse;
import com.gerson.coworking.domain.dto.auth.UserInfo;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.security.JwtTokenProvider;
import com.gerson.coworking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthController(UserService userService,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider jwtTokenProvider,
                         JwtProperties jwtProperties) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                jwtProperties.getExpiration(),
                userInfo
        );

        return ResponseEntity.ok(response);
    }
}
