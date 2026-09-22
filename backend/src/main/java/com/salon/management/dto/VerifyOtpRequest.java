package com.salon.management.dto;

import com.salon.management.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** POST /api/auth/verify-otp body. REGISTER completes a new account (and
 * returns a session); LOGIN finishes a password-checked login (returns JWT). */
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required.")
    private String email;

    @NotBlank(message = "Code is required.")
    @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits.")
    private String code;

    @NotNull(message = "Purpose is required.")
    private OtpPurpose purpose;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }
}
