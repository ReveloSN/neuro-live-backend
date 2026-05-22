package com.neurolive.neuro_live_backend.presentation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verifica que el health global no caiga por SMTP cuando el sistema usa modo logging.
@SpringBootTest(properties = {
        "account-recovery.mail.mode=logging",
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=1",
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=true",
        "spring.mail.properties.mail.smtp.connectiontimeout=50",
        "spring.mail.properties.mail.smtp.timeout=50",
        "spring.mail.properties.mail.smtp.writetimeout=50"
})
@AutoConfigureMockMvc
class HealthEndpointMailIndicatorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldKeepActuatorHealthUpWhenSmtpIsNotConfiguredForLoggingMode() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
