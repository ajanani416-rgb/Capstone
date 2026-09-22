package com.salon.management.dto;

import com.salon.management.entity.Role;
import com.salon.management.entity.User;
import java.time.LocalDateTime;

/** Public identity view — never includes the password hash. */
public class UserResponse {

    private final Long id;
    private final String name;
    private final String phone;
    private final Role role;
    private final LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
