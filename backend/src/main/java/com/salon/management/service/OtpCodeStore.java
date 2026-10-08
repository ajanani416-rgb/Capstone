package com.salon.management.service;

import com.salon.management.entity.OtpCode;
import com.salon.management.repository.OtpCodeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * OTP bookkeeping in its own REQUIRES_NEW transaction. Verification throws on
 * failure, which would roll back attempt/used updates made in the same
 * transaction — lockout and single-use would silently never happen. Every
 * mutating call here commits independently.
 */
@Component
public class OtpCodeStore {

    private final OtpCodeRepository codes;

    public OtpCodeStore(OtpCodeRepository codes) {
        this.codes = codes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordMismatch(Long id) {
        OtpCode otp = codes.findById(id).orElseThrow();
        otp.setAttempts(otp.getAttempts() + 1);
        if (otp.getAttempts() >= OtpCode.MAX_ATTEMPTS) {
            otp.setUsed(true);
        }
        codes.save(otp);
        return otp.getAttempts();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUsed(Long id) {
        OtpCode otp = codes.findById(id).orElseThrow();
        otp.setUsed(true);
        codes.save(otp);
    }
}
