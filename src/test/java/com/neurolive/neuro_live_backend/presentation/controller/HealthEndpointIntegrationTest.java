package com.neurolive.neuro_live_backend.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// Verifica que el health check principal siga disponible.
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Comprueba que el actuator responda con estado UP.
    @Test
    void shouldExposeHealthyActuatorEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
