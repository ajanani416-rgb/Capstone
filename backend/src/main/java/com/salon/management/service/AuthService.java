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
 * Email-OTP auth (TASK-009). No JWT is issued without a verified code:
 * register creates an UNVERIFIED customer and returns a challenge; login
 * checks the password and returns a challenge. Verification exchanges the
 * code for the session. Registration still always creates CUSTOMER (D4).
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
            JwtService jwtService, OtpService otpService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    @Transactional
    public OtpChallengeResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        User user = new User(request.getName().trim(), email,
                passwordEncoder.encode(request.getPassword()), Role.CUSTOMER);
        user.setVerified(false);
        users.save(user);
        return otpService.issue(email, OtpPurpose.REGISTER, "account registration");
    }

    @Transactional
    public OtpChallengeResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!Boolean.TRUE.equals(user.getVerified())) {
            // Fresh REGISTER code + 403 the UI can branch on (status alone).
            otpService.issue(email, OtpPurpose.REGISTER, "account registration");
            throw new UnverifiedEmailException(email);
        }
        return otpService.issue(email, OtpPurpose.LOGIN, "login");
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        otpService.verify(request.getEmail(), request.getPurpose(), request.getCode());
        String email = request.getEmail().trim().toLowerCase();
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found."));
        if (request.getPurpose() == OtpPurpose.REGISTER) {
            user.setVerified(true);
            users.save(user);
        } else if (!Boolean.TRUE.equals(user.getVerified())) {
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
            if (Boolean.TRUE.equals(user.getVerified())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Email is already verified. Log in instead.");
            }
            return otpService.issue(email, OtpPurpose.REGISTER, "account registration");
        }
        if (!Boolean.TRUE.equals(user.getVerified())) {
            // Same generic answer as bad credentials: never reveal registration.
            throw new InvalidCredentialsException();
        }
        return otpService.issue(email, OtpPurpose.LOGIN, "login");
    }

    private AuthResponse tokenFor(User user) {
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(),
                user.getEmail(), user.getRole());
    }
}
