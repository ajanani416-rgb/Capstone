package com.salon.management.dto;

import com.salon.management.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** POST /api/auth/otp/resend body. Invalidates the current live code (if any)
 * and issues a fresh one. */
public class ResendOtpRequest {

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Enter a valid phone number.")
    private String phone;

    @NotNull(message = "Purpose is required.")
    private OtpPurpose purpose;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }
}
