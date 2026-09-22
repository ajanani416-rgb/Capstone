package com.salon.management.dto;

import com.salon.management.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** POST /api/auth/otp/resend body. Invalidates the current live code (if any)
 * and issues a fresh one. */
public class ResendOtpRequest {

    @NotBlank(message = "Email is required.")
    private String email;

    @NotNull(message = "Purpose is required.")
    private OtpPurpose purpose;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }
}
