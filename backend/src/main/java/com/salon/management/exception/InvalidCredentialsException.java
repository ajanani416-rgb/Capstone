package com.salon.management.exception;

/** Unknown email or wrong password → 401 Unauthorized. The message is
 * deliberately generic so callers can't probe which emails are registered. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
