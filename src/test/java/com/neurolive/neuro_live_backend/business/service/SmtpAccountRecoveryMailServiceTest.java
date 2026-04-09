package com.neurolive.neuro_live_backend.business.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
// Asegura que la implementacion SMTP construya un correo util para el usuario.
class SmtpAccountRecoveryMailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void sendRecoveryTokenShouldBuildSimpleMailMessage() {
        SmtpAccountRecoveryMailService mailService = new SmtpAccountRecoveryMailService(
                javaMailSender,
                "no-reply@neurolive.test",
                "Recuperacion NeuroLive",
                "NeuroLive"
        );

        mailService.sendRecoveryToken(
                "patient@neurolive.test",
                "ABC123TOKEN",
                LocalDateTime.of(2026, 4, 9, 20, 30)
        );

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@neurolive.test", message.getFrom());
        assertEquals("Recuperacion NeuroLive", message.getSubject());
        assertEquals("patient@neurolive.test", message.getTo()[0]);
        assertTrue(message.getText().contains("ABC123TOKEN"));
        assertTrue(message.getText().contains("NeuroLive"));
    }
}
