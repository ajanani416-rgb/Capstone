package com.salon.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** POST /api/auth/register body. Role is intentionally absent — public
 * registration always creates CUSTOMER (decision D4). Admins come from the
 * V2 seed migration. Phone is the login identity and the SMS OTP channel. */
public class RegisterRequest {

    @NotBlank(message = "Name is required.")
    @Size(max = 255, message = "Name must be at most 255 characters.")
    private String name;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Enter a valid phone number.")
    @Size(max = 20, message = "Phone number must be at most 20 characters.")
    private String phone;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 100, message = "Password must be 8–100 characters.")
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
