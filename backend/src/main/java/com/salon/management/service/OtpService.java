package com.salon.management.service;

import com.salon.management.dto.OtpChallengeResponse;
import com.salon.management.entity.OtpCode;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.exception.InvalidOtpException;
import com.salon.management.exception.OtpExpiredException;
import com.salon.management.exception.OtpResendCooldownException;
import com.salon.management.mail.EmailSender;
import com.salon.management.repository.OtpCodeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email OTP lifecycle. Codes are 6 digits (SecureRandom), SHA-256 hashed at
 * rest, 10-minute TTL, 5 attempts, single-active-code per (email, purpose),
 * 60-second resend cooldown. Plaintexts never touch the database, logs, or
 * API responses — only the email body in transit. Delivery failures throw,
 * rolling back the surrounding registration/resend transaction.
 */
@Service
public class OtpService {

    private final OtpCodeRepository codes;
    private final OtpCodeStore store;
    private final EmailSender emailSender;
    private final long ttlMinutes;
    private final long resendCooldownSeconds;
    private final boolean exposeLastCode;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> lastIssued = new ConcurrentHashMap<>();

    public OtpService(OtpCodeRepository codes, OtpCodeStore store, EmailSender emailSender,
            @Value("${app.otp.ttl-minutes:10}") long ttlMinutes,
            @Value("${app.otp.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${app.otp.expose-last-code:false}") boolean exposeLastCode) {
        this.codes = codes;
        this.store = store;
        this.emailSender = emailSender;
        this.ttlMinutes = ttlMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.exposeLastCode = exposeLastCode;
    }

    /** Issues a code, invalidating any previous unused one for the pair. */
    @Transactional
    public OtpChallengeResponse issue(String email, OtpPurpose purpose, String name) {
        String normalized = email.trim().toLowerCase();
        codes.findByEmailAndPurposeAndUsedFalse(normalized, purpose)
                .forEach(old -> old.setUsed(true));
        String code = String.format("%06d", random.nextInt(900000) + 100000);
        OtpCode otp = new OtpCode(normalized, sha256(code), purpose,
                LocalDateTime.now().plusMinutes(ttlMinutes));
        codes.save(otp);
        emailSender.sendVerificationCode(normalized, name, code);
        if (exposeLastCode) {
            // TEST-ONLY hook (app.otp.expose-last-code, true in test properties).
            // Lets API tests complete the flow without reading an inbox.
            lastIssued.put(key(normalized, purpose), code);
        }
        return new OtpChallengeResponse(normalized, purpose, ttlMinutes * 60);
    }

    /** Rejects resends inside the cooldown window (429). Checks the latest
     * code of any state — used or not — so rapid retries always throttle. */
    @Transactional(readOnly = true)
    public void checkResendCooldown(String email, OtpPurpose purpose) {
        String normalized = email.trim().toLowerCase();
        codes.findFirstByEmailAndPurposeOrderByCreatedAtDesc(normalized, purpose)
                .ifPresent(latest -> {
                    long elapsed = Duration.between(latest.getCreatedAt(),
                            LocalDateTime.now()).getSeconds();
                    if (elapsed < resendCooldownSeconds) {
                        throw new OtpResendCooldownException();
                    }
                });
    }

    /** Validates a code. Wrong → 401 (no leakage); dead (expired/used up) → 410.
     * Bookkeeping commits via OtpCodeStore: this method throws, so same-txn
     * updates would roll back and lockout would never accumulate. */
    @Transactional
    public void verify(String email, OtpPurpose purpose, String code) {
        String normalized = email.trim().toLowerCase();
        OtpCode otp = codes
                .findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(normalized, purpose)
                .orElseThrow(InvalidOtpException::new);
        if (!otp.isLive()) {
            store.markUsed(otp.getId());
            throw new OtpExpiredException();
        }
        if (!otp.getCodeHash().equals(sha256(code.trim()))) {
            int attempts = store.recordMismatch(otp.getId());
            if (attempts >= OtpCode.MAX_ATTEMPTS) {
                throw new OtpExpiredException();
            }
            throw new InvalidOtpException();
        }
        store.markUsed(otp.getId());
    }

    /** Test-only accessor; always null unless expose-last-code is enabled. */
    public String lastIssuedCode(String email, OtpPurpose purpose) {
        return lastIssued.get(key(email.trim().toLowerCase(), purpose));
    }

    private static String key(String email, OtpPurpose purpose) {
        return email + "|" + purpose.name();
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
