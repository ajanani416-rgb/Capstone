package com.salon.management.exception;

/** Expired, used-up, or attempts-exhausted code → 410 Gone. The remedy is
 * always the same: request a fresh code. */
public class OtpExpiredException extends RuntimeException {

    public OtpExpiredException() {
        super("That code expired or was used too many times. Request a new one.");
    }
}
