package com.salon.management.dto;

import com.salon.management.entity.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** POST /api/auth/verify-otp body. REGISTER completes a new account (marks
 * the email verified and returns a session); LOGIN is unused — verified
 * users authenticate with email+password directly. */
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 255)
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
