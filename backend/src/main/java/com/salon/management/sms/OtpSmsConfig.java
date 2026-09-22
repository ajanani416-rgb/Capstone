package com.salon.management.sms;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the OTP SMS sender. Console carries demos and local dev; a future
 * gateway sender (Twilio, etc.) takes precedence when app.sms.gateway is set,
 * mirroring the old mail fallback ordering. Both conditions live in this one
 * class so the fallback ordering is deterministic. */
@Configuration
public class OtpSmsConfig {

    @Bean
    public ConsoleOtpSmsSender consoleOtpSmsSender() {
        return new ConsoleOtpSmsSender();
    }
}
