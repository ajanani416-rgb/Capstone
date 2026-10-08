package com.salon.management.mail;

/** Sends one-time verification codes by email. The SMTP implementation is
 * the only production sender; tests replace this interface with an
 * in-memory fake. The OTP value only ever exists in memory + transit +
 * hashed in the database — never in logs, never in API responses. */
public interface EmailSender {

    /** Sends the verification code email. Throws EmailSendFailedException
     * when the SMTP provider rejects or is unreachable. */
    void sendVerificationCode(String toEmail, String name, String code);
}
