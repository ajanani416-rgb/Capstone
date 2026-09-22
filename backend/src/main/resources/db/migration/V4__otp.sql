-- V4: email-OTP verification (TASK-009: OTP before register-complete and
-- before every login). Pre-OTP accounts are grandfathered as verified so the
-- seeded admin and existing demo users keep working.
ALTER TABLE users ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET verified = TRUE;

CREATE TABLE IF NOT EXISTS otp_codes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    expires_at DATETIME NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    INDEX idx_otp_email_purpose (email, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
