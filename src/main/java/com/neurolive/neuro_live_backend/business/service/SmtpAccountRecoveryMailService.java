package com.neurolive.neuro_live_backend.business.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(prefix = "account-recovery.mail", name = "mode", havingValue = "smtp")
public class SmtpAccountRecoveryMailService implements AccountRecoveryMailService {

    private static final DateTimeFormatter EXPIRATION_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender javaMailSender;
    private final String fromAddress;
    private final String subject;
    private final String applicationName;

    public SmtpAccountRecoveryMailService(JavaMailSender javaMailSender,
                                        @Value("${account-recovery.mail.from:${spring.mail.username:no-reply@neurolive.local}}")
                                        String fromAddress,
                                        @Value("${account-recovery.mail.subject:Codigo de recuperacion NeuroLive}")
                                        String subject,
                                        @Value("${account-recovery.mail.application-name:NeuroLive}")
                                        String applicationName) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress;
        this.subject = subject;
        this.applicationName = applicationName;
    }

    @Override
    public void sendRecoveryToken(String email, String rawToken, LocalDateTime expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(buildBody(rawToken, expiresAt));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException("Recovery instructions could not be delivered", exception);
        }
    }

    private String buildBody(String rawToken, LocalDateTime expiresAt) {
        return """
                Hola,

                Recibimos una solicitud para restablecer tu contrasena en %s.

                Usa este codigo temporal para continuar:
                %s

                El codigo vence el %s.
                Si no solicitaste este cambio, puedes ignorar este correo.
                """.formatted(
                applicationName,
                rawToken,
                expiresAt == null ? "pronto" : expiresAt.format(EXPIRATION_FORMATTER)
        );
    }
}
