package com.salon.management.exception;

/** Unknown phone number → 401 Unauthorized. The message is deliberately
 * generic so callers can't probe which numbers are registered. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid phone number.");
    }
}
