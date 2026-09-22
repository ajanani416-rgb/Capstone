package com.salon.management.dto;

import com.salon.management.entity.OtpPurpose;

/** Returned when a code is issued (register / login-password-ok / resend).
 * Carries no token — the JWT only arrives after successful verification. */
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
