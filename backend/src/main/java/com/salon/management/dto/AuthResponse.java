package com.salon.management.dto;

import com.salon.management.entity.Role;

/** Success payload for OTP verification: identity + bearer token. The frontend
 * stores the token and sends it as `Authorization: Bearer <token>`. */
public class AuthResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final Long id;
    private final String name;
    private final String phone;
    private final Role role;

    public AuthResponse(String token, Long id, String name, String phone, Role role) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }
}
