package com.salon.management.dto;

import com.salon.management.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** POST /api/auth/verify-otp body. REGISTER completes a new account (and
 * returns a session); LOGIN finishes a number-lookup login (returns JWT). */
public class VerifyOtpRequest {

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Enter a valid phone number.")
    private String phone;

    @NotBlank(message = "Code is required.")
    @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits.")
    private String code;

    @NotNull(message = "Purpose is required.")
    private OtpPurpose purpose;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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
