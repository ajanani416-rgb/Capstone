package com.salon.management.exception;

/** Wrong/unknown code → 401. Deliberately identical whether the code never
 * existed or simply mismatches, so attackers can't probe. */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("Invalid verification code.");
    }
}
