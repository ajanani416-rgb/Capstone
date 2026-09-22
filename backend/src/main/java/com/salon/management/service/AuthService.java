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
import com.salon.management.exception.DuplicatePhoneException;
import com.salon.management.exception.InvalidCredentialsException;
import com.salon.management.exception.UnverifiedPhoneException;
import com.salon.management.repository.UserRepository;
import com.salon.management.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phone-OTP auth. No JWT is issued without a verified code: register creates
 * an UNVERIFIED customer and returns a challenge; login checks the password
 * and returns a challenge. Verification exchanges the code for the session.
 * Registration still always creates CUSTOMER (D4). The code travels by SMS
 * (console sender in dev, gateway later) — no email anywhere in this flow.
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
        String phone = request.getPhone().trim();
        if (users.existsByPhone(phone)) {
            throw new DuplicatePhoneException(phone);
        }
        User user = new User(request.getName().trim(), phone,
                passwordEncoder.encode(request.getPassword()), Role.CUSTOMER);
        user.setVerified(false);
        users.save(user);
        return otpService.issue(phone, OtpPurpose.REGISTER, "account registration");
    }

    @Transactional
    public OtpChallengeResponse login(LoginRequest request) {
        String phone = request.getPhone().trim();
        User user = users.findByPhone(phone).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!Boolean.TRUE.equals(user.getVerified())) {
            // Fresh REGISTER code + 403 the UI can branch on (status alone).
            otpService.issue(phone, OtpPurpose.REGISTER, "account registration");
            throw new UnverifiedPhoneException(phone);
        }
        return otpService.issue(phone, OtpPurpose.LOGIN, "login");
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        otpService.verify(request.getPhone(), request.getPurpose(), request.getCode());
        String phone = request.getPhone().trim();
        User user = users.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found."));
        if (request.getPurpose() == OtpPurpose.REGISTER) {
            user.setVerified(true);
            users.save(user);
        } else if (!Boolean.TRUE.equals(user.getVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Phone number is not verified yet. Complete registration first.");
        }
        return tokenFor(user);
    }

    @Transactional
    public OtpChallengeResponse resend(ResendOtpRequest request) {
        String phone = request.getPhone().trim();
        User user = users.findByPhone(phone).orElseThrow(InvalidCredentialsException::new);
        if (request.getPurpose() == OtpPurpose.REGISTER) {
            if (Boolean.TRUE.equals(user.getVerified())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Phone number is already verified. Log in instead.");
            }
            return otpService.issue(phone, OtpPurpose.REGISTER, "account registration");
        }
        if (!Boolean.TRUE.equals(user.getVerified())) {
            // Same generic answer as bad credentials: never reveal registration.
            throw new InvalidCredentialsException();
        }
        return otpService.issue(phone, OtpPurpose.LOGIN, "login");
    }

    private AuthResponse tokenFor(User user) {
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(),
                user.getPhone(), user.getRole());
    }
}
