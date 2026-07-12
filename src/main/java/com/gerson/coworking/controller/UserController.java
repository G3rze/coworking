package com.gerson.coworking.controller;

import com.gerson.coworking.domain.dto.user.UserCreateRequest;
import com.gerson.coworking.domain.dto.user.UserResponse;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        if (userService.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userService.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userService.createUser(request);

        UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        throw new UnsupportedOperationException("List users not implemented yet");
    }
}
