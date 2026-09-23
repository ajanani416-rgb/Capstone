package com.salon.management.mail;

import com.salon.management.exception.EmailSendFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** SMTP sender (Gmail in the operator setup). HTML body with a plain-text
 * fallback; subject fixed per spec. Never includes passwords or JWTs — only
 * the code, the name, and the expiry. SMTP credentials live in env-backed
 * properties and never reach this class's logs. */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public SmtpEmailSender(JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.from-name:Smart Salon}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    @Override
    public void sendVerificationCode(String toEmail, String name, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // Multipart mode is required for setText(plain, html) alternatives.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Smart Salon \u2014 Verify your email");
            helper.setText(textBody(name, code), htmlBody(name, code));
            mailSender.send(message);
        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException e) {
            throw new EmailSendFailedException();
        }
    }

    private static String textBody(String name, String code) {
        return "Hello " + name + ",\n\n"
                + "Welcome to Smart Salon.\n\n"
                + "Your email verification code is:\n\n"
                + code + "\n\n"
                + "This code expires in 10 minutes.\n\n"
                + "If you did not create a Smart Salon account, you can safely ignore this email.\n\n"
                + "Smart Salon";
    }

    private static String htmlBody(String name, String code) {
        return "<p>Hello " + escape(name) + ",</p>"
                + "<p>Welcome to Smart Salon.</p>"
                + "<p>Your email verification code is:</p>"
                + "<p style=\"font-size:24px;font-weight:bold;letter-spacing:4px;\">"
                + escape(code) + "</p>"
                + "<p>This code expires in 10 minutes.</p>"
                + "<p>If you did not create a Smart Salon account, you can safely ignore this email.</p>"
                + "<p>Smart Salon</p>";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
