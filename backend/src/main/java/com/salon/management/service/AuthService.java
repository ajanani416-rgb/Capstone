package com.salon.management.service;

import com.salon.management.dto.AuthResponse;
import com.salon.management.dto.LoginRequest;
import com.salon.management.dto.OtpChallengeResponse;
import com.salon.management.dto.RegisterRequest;
import com.salon.management.dto.ResendOtpRequest;
import com.salon.management.dto.VerifyOtpRequest;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.entity.Role;
import com.salon.management.entity.User;
import com.salon.management.exception.DuplicateEmailException;
import com.salon.management.exception.InvalidCredentialsException;
import com.salon.management.exception.UnverifiedEmailException;
import com.salon.management.repository.UserRepository;
import com.salon.management.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Email + password auth with email-OTP verification. No JWT is issued to an
 * unverified account: register creates an UNVERIFIED customer and emails a
 * code; login checks the BCrypt password, then refuses unverified accounts
 * with 403 (issuing a fresh code). Verification flips the flag and returns
 * the session. Registration still always creates CUSTOMER (D4).
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, JwtService jwtService, OtpService otpService,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OtpChallengeResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        User user = new User(request.getName().trim(), email,
                passwordEncoder.encode(request.getPassword()), Role.CUSTOMER);
        user.setEmailVerified(false);
        users.save(user);
        return otpService.issue(email, OtpPurpose.REGISTER, user.getName());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        // Same generic answer for unknown email and wrong password: never
        // reveal which emails are registered.
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            // Fresh REGISTER code + 403 the UI can branch on (status alone).
            otpService.issue(email, OtpPurpose.REGISTER, user.getName());
            throw new UnverifiedEmailException(email);
        }
        return tokenFor(user);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        otpService.verify(request.getEmail(), request.getPurpose(), request.getCode());
        String email = request.getEmail().trim().toLowerCase();
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found."));
        if (request.getPurpose() == OtpPurpose.REGISTER) {
            user.setEmailVerified(true);
            users.save(user);
        } else if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Email is not verified yet. Complete registration first.");
        }
        return tokenFor(user);
    }

    @Transactional
    public OtpChallengeResponse resend(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (request.getPurpose() == OtpPurpose.REGISTER) {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Email is already verified. Log in instead.");
            }
            otpService.checkResendCooldown(email, OtpPurpose.REGISTER);
            return otpService.issue(email, OtpPurpose.REGISTER, user.getName());
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            // Same generic answer as unknown email: never reveal registration.
            throw new InvalidCredentialsException();
        }
        otpService.checkResendCooldown(email, OtpPurpose.LOGIN);
        return otpService.issue(email, OtpPurpose.LOGIN, user.getName());
    }

    private AuthResponse tokenFor(User user) {
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(),
                user.getEmail(), user.getRole());
    }
}
