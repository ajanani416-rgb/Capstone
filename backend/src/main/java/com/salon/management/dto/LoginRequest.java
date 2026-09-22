package com.salon.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** POST /api/auth/login body. Phone + password; a verified code follows. */
public class LoginRequest {

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Enter a valid phone number.")
    private String phone;

    @NotBlank(message = "Password is required.")
    private String password;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
