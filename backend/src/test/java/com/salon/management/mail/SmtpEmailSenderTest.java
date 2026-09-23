package com.salon.management.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.salon.management.exception.EmailSendFailedException;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Unit tests for the real SMTP sender — no Spring context, no network.
 * The capturing transport records instead of connecting. Exists because the
 * multipart-mode bug (500 instead of 502) slipped past the mocked API tests:
 * no test ever instantiated this class. */
class SmtpEmailSenderTest {

    /** JavaMailSender that captures instead of connecting. */
    private static class CapturingSender extends JavaMailSenderImpl {
        final List<MimeMessage> sent = new ArrayList<>();

        @Override
        public void send(MimeMessage mimeMessage) {
            sent.add(mimeMessage);
        }
    }

    @Test
    void sendsMultipartAlternativeWithCode() throws Exception {
        CapturingSender transport = new CapturingSender();
        SmtpEmailSender sender =
                new SmtpEmailSender(transport, "salon@example.com", "Smart Salon");

        sender.sendVerificationCode("user@example.com", "Asha", "482913");

        assertThat(transport.sent).hasSize(1);
        MimeMessage message = transport.sent.get(0);
        // Transport.saveChanges() finalizes part headers on real sends.
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo("Smart Salon \u2014 Verify your email");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
        Object content = message.getContent();
        assertThat(content).isInstanceOf(MimeMultipart.class);
        // Walk any wrapper nesting (mixed/related/alternative) to the leaves.
        List<String> bodies = new ArrayList<>();
        collectLeaves((MimeMultipart) content, bodies);
        assertThat(bodies).hasSize(2);
        assertThat(bodies).anySatisfy(b -> assertThat(b)
                .contains("text/plain").contains("482913").contains("Asha"));
        assertThat(bodies).anySatisfy(b -> assertThat(b)
                .contains("text/html").contains("482913"));
    }

    private static void collectLeaves(MimeMultipart multipart, List<String> bodies)
            throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object nested = part.getContent();
            if (nested instanceof MimeMultipart inner) {
                collectLeaves(inner, bodies);
            } else {
                bodies.add(part.getContentType() + "\n" + nested.toString());
            }
        }
    }

    @Test
    void transportFailureBecomesEmailSendFailed() {
        JavaMailSenderImpl transport = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) {
                throw new MailSendException("smtp down");
            }
        };
        SmtpEmailSender sender =
                new SmtpEmailSender(transport, "salon@example.com", "Smart Salon");

        assertThatThrownBy(
                () -> sender.sendVerificationCode("user@example.com", "Asha", "482913"))
                .isInstanceOf(EmailSendFailedException.class);
    }
}
