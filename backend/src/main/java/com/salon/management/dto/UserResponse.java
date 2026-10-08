package com.salon.management.dto;

import com.salon.management.entity.Role;
import com.salon.management.entity.User;
import java.time.LocalDateTime;

/** Public identity view — never includes the password hash. */
public class UserResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final Role role;
    private final LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
