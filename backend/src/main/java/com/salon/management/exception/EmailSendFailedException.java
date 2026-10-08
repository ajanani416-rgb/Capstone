package com.salon.management.exception;

/** SMTP rejected or is unreachable during code delivery → 502 Bad Gateway.
 * The message stays generic: provider responses can leak account details. */
public class EmailSendFailedException extends RuntimeException {

    public EmailSendFailedException() {
        super("We could not send the verification email. Please retry.");
    }
}
