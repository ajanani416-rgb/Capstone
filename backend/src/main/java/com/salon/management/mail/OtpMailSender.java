package com.salon.management.mail;

/** Sends one-time passcodes. Two implementations: console (default, demo and
 * local dev — the code is logged, never secret in a shared mailbox sense)
 * and SMTP (production, active when app.mail.host is set). The OTP value
 * itself only ever exists in memory + transit + hashed in the database. */
public interface OtpMailSender {

    void sendCode(String email, String code, String purposeLabel);
}
