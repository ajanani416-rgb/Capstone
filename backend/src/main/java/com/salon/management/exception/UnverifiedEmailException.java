package com.salon.management.exception;

/** Login with correct password but unverified email → 403. A fresh REGISTER
 * code is issued alongside, so the UI can jump straight to the OTP step on
 * this status alone (no other login path answers 403). */
public class UnverifiedEmailException extends RuntimeException {

    public UnverifiedEmailException(String email) {
        super("Email " + email + " is not verified yet. Enter the code we sent you.");
    }
}
