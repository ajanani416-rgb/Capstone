package com.salon.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/auth/register body. Role is intentionally absent — public
 * registration always creates CUSTOMER (decision D4). Admins come from the
 * V2 seed migration. */
public class RegisterRequest {

    @NotBlank(message = "Name is required.")
    @Size(max = 255, message = "Name must be at most 255 characters.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 255, message = "Email must be at most 255 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 100, message = "Password must be 8–100 characters.")
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
