package com.salon.management.dto;

import com.salon.management.entity.Role;

/** Success payload for OTP verification: identity + bearer token. The frontend
 * stores the token and sends it as `Authorization: Bearer <token>`. */
public class AuthResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final Long id;
    private final String name;
    private final String email;
    private final Role role;

    public AuthResponse(String token, Long id, String name, String email, Role role) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
