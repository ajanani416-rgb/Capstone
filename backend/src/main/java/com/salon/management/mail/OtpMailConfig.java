package com.salon.management.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Chooses the OTP sender. SMTP wins when app.mail.host is set; otherwise the
 * console sender carries demos and local dev. Both conditions live in this
 * one class so the fallback ordering is deterministic. */
@Configuration
public class OtpMailConfig {

    @Bean
    @ConditionalOnProperty(name = "app.mail.host")
    public SmtpOtpMailSender smtpOtpMailSender(
            @Value("${app.mail.host}") String host,
            @Value("${app.mail.port:587}") int port,
            @Value("${app.mail.username:}") String username,
            @Value("${app.mail.password:}") String password,
            @Value("${app.mail.from:Smart Salon <no-reply@salon.local>}") String from) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        var props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return new SmtpOtpMailSender(sender, from);
    }

    @Bean
    @ConditionalOnMissingBean(SmtpOtpMailSender.class)
    public ConsoleOtpMailSender consoleOtpMailSender() {
        return new ConsoleOtpMailSender();
    }
}
