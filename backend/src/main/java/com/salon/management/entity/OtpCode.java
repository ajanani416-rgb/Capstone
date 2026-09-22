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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * One pending SMS OTP. Only the SHA-256 hash is stored — a database leak
 * never reveals usable codes. At most one unused code per (phone, purpose);
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
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Enter a valid phone number.")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String phone;

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

    public OtpCode(String phone, String codeHash, OtpPurpose purpose, LocalDateTime expiresAt) {
        this.phone = phone;
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

    public String getPhone() {
        return phone;
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
