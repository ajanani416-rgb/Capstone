package com.salon.management.exception;

/** Login with an unverified phone → 403. A fresh REGISTER
 * code is issued alongside, so the UI can jump straight to the OTP step on
 * this status alone (no other login path answers 403). */
public class UnverifiedPhoneException extends RuntimeException {

    public UnverifiedPhoneException(String phone) {
        super("Phone number " + phone + " is not verified yet. Enter the code we texted you.");
    }
}
