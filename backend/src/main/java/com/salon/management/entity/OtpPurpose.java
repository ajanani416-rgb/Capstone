package com.salon.management.entity;

/** What an OTP code unlocks. REGISTER completes a new account; LOGIN finishes
 * a password-checked login. Separate codes per purpose — a register code can
 * never log anyone in and vice versa. */
public enum OtpPurpose {
    REGISTER,
    LOGIN
}
