package com.salon.management.exception;

/** Registration with an already-registered email → 409 Conflict. */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email " + email + " already exists.");
    }
}
