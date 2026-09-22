package com.salon.management.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default sender: logs the code to the application log. Correct for local
 * dev, demos, and capstone review — the reviewer reads the code from the
 * backend console. MUST NOT be used with real user data in production; set
 * app.mail.host to switch to SMTP (see SmtpOtpMailSender). Wired by
 * OtpMailConfig: console is the fallback when no SMTP host is set. */
public class ConsoleOtpMailSender implements OtpMailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleOtpMailSender.class);

    @Override
    public void sendCode(String email, String code, String purposeLabel) {
        // Deliberately logged at INFO so it is visible in `mvn spring-boot:run`
        // output and container logs during the demo.
        log.info("OTP for {} ({}): {}", email, purposeLabel, code);
    }
}
