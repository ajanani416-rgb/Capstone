package com.salon.management.dto;

import com.salon.management.entity.OtpPurpose;

/** Returned when a code is emailed (register / resend). Carries no token —
 * the JWT only arrives after successful verification (register) or direct
 * password login. */
public class OtpChallengeResponse {

    private final String email;
    private final OtpPurpose purpose;
    private final long expiresInSeconds;

    public OtpChallengeResponse(String email, OtpPurpose purpose, long expiresInSeconds) {
        this.email = email;
        this.purpose = purpose;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getEmail() {
        return email;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
