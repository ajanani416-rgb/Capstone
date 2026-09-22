package com.salon.management.dto;

import com.salon.management.entity.OtpPurpose;

/** Returned when a code is issued (register / login-password-ok / resend).
 * Carries no token — the JWT only arrives after successful verification. */
public class OtpChallengeResponse {

    private final String phone;
    private final OtpPurpose purpose;
    private final long expiresInSeconds;

    public OtpChallengeResponse(String phone, OtpPurpose purpose, long expiresInSeconds) {
        this.phone = phone;
        this.purpose = purpose;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getPhone() {
        return phone;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
