package com.salon.management.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Production sender, constructed by OtpMailConfig when app.mail.host is set
 * (e.g. smtp.gmail.com with an app password — see README). */
public class SmtpOtpMailSender implements OtpMailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpOtpMailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendCode(String email, String code, String purposeLabel) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Your Smart Salon verification code");
        message.setText("Your verification code for " + purposeLabel + " is: " + code
                + "\nIt expires in 10 minutes. If you did not request it, ignore this email.");
        mailSender.send(message);
    }
}
