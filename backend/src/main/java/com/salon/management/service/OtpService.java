package com.salon.management.service;

import com.salon.management.dto.OtpChallengeResponse;
import com.salon.management.entity.OtpCode;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.exception.InvalidOtpException;
import com.salon.management.exception.OtpExpiredException;
import com.salon.management.repository.OtpCodeRepository;
import com.salon.management.sms.OtpSmsSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SMS OTP lifecycle. Codes are 6 digits, SHA-256 hashed at rest, 2-minute
 * TTL, 5 attempts, single-active-code per (phone, purpose). Plaintexts never
 * touch the database or logs — except the SMS sender, whose console
 * implementation is demo-only by design.
 */
@Service
public class OtpService {

    private final OtpCodeRepository codes;
    private final OtpCodeStore store;
    private final OtpSmsSender smsSender;
    private final long ttlMinutes;
    private final boolean exposeLastCode;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> lastIssued = new ConcurrentHashMap<>();

    public OtpService(OtpCodeRepository codes, OtpCodeStore store, OtpSmsSender smsSender,
            @Value("${app.otp.ttl-minutes:10}") long ttlMinutes,
            @Value("${app.otp.expose-last-code:false}") boolean exposeLastCode) {
        this.codes = codes;
        this.store = store;
        this.smsSender = smsSender;
        this.ttlMinutes = ttlMinutes;
        this.exposeLastCode = exposeLastCode;
    }

    /** Issues a code, invalidating any previous unused one for the pair. */
    @Transactional
    public OtpChallengeResponse issue(String phone, OtpPurpose purpose, String purposeLabel) {
        String normalized = phone.trim();
        codes.findByPhoneAndPurposeAndUsedFalse(normalized, purpose)
                .forEach(old -> old.setUsed(true));
        String code = String.format("%06d", random.nextInt(900000) + 100000);
        OtpCode otp = new OtpCode(normalized, sha256(code), purpose,
                LocalDateTime.now().plusMinutes(ttlMinutes));
        codes.save(otp);
        smsSender.sendCode(normalized, code, purposeLabel);
        if (exposeLastCode) {
            // TEST-ONLY hook (app.otp.expose-last-code, true in test properties).
            // Lets API tests complete the flow without reading an SMS.
            lastIssued.put(key(normalized, purpose), code);
        }
        return new OtpChallengeResponse(normalized, purpose, ttlMinutes * 60);
    }

    /** Validates a code. Wrong → 401 (no leakage); dead (expired/used up) → 410.
     * Bookkeeping commits via OtpCodeStore: this method throws, so same-txn
     * updates would roll back and lockout would never accumulate. */
    @Transactional
    public void verify(String phone, OtpPurpose purpose, String code) {
        String normalized = phone.trim();
        OtpCode otp = codes
                .findFirstByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(normalized, purpose)
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
    public String lastIssuedCode(String phone, OtpPurpose purpose) {
        return lastIssued.get(key(phone.trim(), purpose));
    }

    private static String key(String phone, OtpPurpose purpose) {
        return phone + "|" + purpose.name();
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
