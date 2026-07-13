package com.gerson.coworking.controller;

import com.gerson.coworking.config.AppProperties;
import com.gerson.coworking.domain.dto.auth.LoginRequest;
import com.gerson.coworking.domain.dto.auth.LoginResponse;
import com.gerson.coworking.domain.dto.auth.UserInfo;
import com.gerson.coworking.domain.entity.User;
import com.gerson.coworking.security.JwtTokenProvider;
import com.gerson.coworking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and JWT token generation")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppProperties appProperties;

    public AuthController(UserService userService,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider jwtTokenProvider,
                         AppProperties appProperties) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appProperties = appProperties;
    }

    @Operation(summary = "Authenticate user", description = "Validates credentials and returns a JWT token for authenticated requests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class)))
    })
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
                appProperties.getJwt().getExpiration(),
                userInfo
        );

        return ResponseEntity.ok(response);
    }
}