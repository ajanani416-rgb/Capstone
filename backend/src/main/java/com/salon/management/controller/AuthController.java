package com.salon.management.controller;

import com.salon.management.dto.AuthResponse;
import com.salon.management.dto.LoginRequest;
import com.salon.management.dto.OtpChallengeResponse;
import com.salon.management.dto.RegisterRequest;
import com.salon.management.dto.ResendOtpRequest;
import com.salon.management.dto.UserResponse;
import com.salon.management.dto.VerifyOtpRequest;
import com.salon.management.entity.User;
import com.salon.management.repository.UserRepository;
import com.salon.management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public: POST register (→ OTP challenge), POST login (lookup → OTP
 * challenge, or 403 unverified with a fresh code), POST verify-otp
 * (code → JWT session), POST otp/resend. Authenticated: GET me.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    public ResponseEntity<OtpChallengeResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<OtpChallengeResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/otp/resend")
    public ResponseEntity<OtpChallengeResponse> resend(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resend(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        long userId = Long.parseLong(authentication.getName());
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(new UserResponse(user));
    }
}
