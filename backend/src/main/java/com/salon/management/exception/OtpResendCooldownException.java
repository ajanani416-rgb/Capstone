package com.salon.management.exception;

/** Resend requested inside the 60-second cooldown → 429 Too Many Requests. */
public class OtpResendCooldownException extends RuntimeException {

    public OtpResendCooldownException() {
        super("A code was just sent. Please wait before requesting a new one.");
    }
}
