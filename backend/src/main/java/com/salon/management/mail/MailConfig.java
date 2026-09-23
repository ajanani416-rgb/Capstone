package com.salon.management.mail;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** SMTP transport (Gmail: smtp.gmail.com:587 + STARTTLS). All values are
 * env-backed (see .env.example); nothing secret is committed. The sender
 * connects lazily on first send, so the app boots fine without credentials —
 * registration simply answers EMAIL_SEND_FAILED until they are configured. */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${app.mail.host:smtp.gmail.com}") String host,
            @Value("${app.mail.port:587}") int port,
            @Value("${app.mail.username:}") String username,
            @Value("${app.mail.password:}") String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
