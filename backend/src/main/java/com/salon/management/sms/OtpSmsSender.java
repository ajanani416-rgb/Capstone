package com.salon.management.sms;

/** Sends one-time passcodes over SMS. Console implementation is the default
 * (demo and local dev — the code is logged, never secret in a shared inbox
 * sense). A real gateway (Twilio, etc.) can replace it later behind this same
 * interface without touching OtpService. The OTP value itself only ever
 * exists in memory + transit + hashed in the database. */
public interface OtpSmsSender {

    void sendCode(String phone, String code, String purposeLabel);
}
