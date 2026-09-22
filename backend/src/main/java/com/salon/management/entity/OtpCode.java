package com.salon.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * One pending email OTP. Only the SHA-256 hash is stored — a database leak
 * never reveals usable codes. At most one unused code per (email, purpose);
 * issuing a new one invalidates the old. Max 5 verification attempts, then
 * the code dies and the client must resend.
 */
@Entity
@Table(name = "otp_codes")
public class OtpCode {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false)
    private String email;

    @NotBlank
    @Size(max = 64)
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected OtpCode() {
    }

    public OtpCode(String email, String codeHash, OtpPurpose purpose, LocalDateTime expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isLive() {
        return !used && attempts < MAX_ATTEMPTS && LocalDateTime.now().isBefore(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
