package com.gema.external.mail;

import com.gema.core.port.PasswordResetMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Sends the reset link over SMTP.
 *
 * <p>Provider-agnostic on purpose: anything speaking SMTP works (a transactional
 * provider, a company relay, Mailtrap in staging) by setting
 * {@code spring.mail.*}, with no code change and no vendor SDK in the build.
 *
 * <p>When no mail host is configured Spring does not create a
 * {@link JavaMailSender} at all, and the link is logged instead. That keeps
 * local development working without an SMTP server — but it means the link
 * lands in the application log, so a deployed environment must configure mail.
 * The startup warning below exists to make that impossible to miss.
 */
@Component
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetMailer.class);

    private final Optional<JavaMailSender> mailSender;
    private final String from;

    public SmtpPasswordResetMailer(Optional<JavaMailSender> mailSender,
                                    @Value("${app.mail.from:nao-responda@gema.app}") String from) {
        this.mailSender = mailSender;
        this.from = from;
        if (mailSender.isEmpty()) {
            log.warn("No mail sender configured (spring.mail.host is unset): password reset links will be "
                    + "written to this log instead of being emailed. Do not run a deployed environment this way.");
        }
    }

    @Override
    public void sendResetLink(String recipient, String resetUrl, Duration validFor) {
        long minutes = Math.max(1, validFor.toMinutes());

        if (mailSender.isEmpty()) {
            log.warn("Password reset link for {} (valid {} min): {}", recipient, minutes, resetUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Redefinir sua senha na GEMA");
        message.setText("""
                Olá,

                Recebemos um pedido para redefinir a senha da sua conta na GEMA.

                Para escolher uma nova senha, acesse:
                %s

                O link vale por %d minutos e só pode ser usado uma vez.

                Se não foi você que pediu, pode ignorar esta mensagem — sua senha
                continua a mesma.
                """.formatted(resetUrl, minutes));

        try {
            mailSender.get().send(message);
        } catch (Exception e) {
            // Never surfaced to the caller: the endpoint answers identically
            // whether or not the address exists, and a delivery failure must not
            // become a way to probe that.
            log.error("Failed to send a password reset email", e);
        }
    }
}
