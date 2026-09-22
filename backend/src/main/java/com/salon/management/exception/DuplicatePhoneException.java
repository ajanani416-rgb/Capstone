package com.salon.management.exception;

/** Registration with an already-registered phone number → 409 Conflict. */
public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException(String phone) {
        super("An account with phone number " + phone + " already exists.");
    }
}
