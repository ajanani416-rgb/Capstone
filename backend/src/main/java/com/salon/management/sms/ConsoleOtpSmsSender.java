package com.salon.management.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default sender: logs the code + salon greeting to the application log.
 * Correct for local dev, demos, and capstone review — the reviewer reads the
 * code from the backend console. MUST NOT be used with real user data in
 * production; add a gateway-backed OtpSmsSender and set app.sms.gateway to
 * switch (see OtpSmsConfig). */
public class ConsoleOtpSmsSender implements OtpSmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleOtpSmsSender.class);

    private static final String GREETING =
        "\uD83D\uDC88  Welcome to the Smart Salon — your appointment queue is just a code away.\n";

    @Override
    public void sendCode(String phone, String code, String purposeLabel) {
        // Visible in `mvn spring-boot:run` output and container logs during the demo.
        log.info("{}SMS OTP for {} ({}): {}", GREETING, phone, purposeLabel, code);
    }
}
