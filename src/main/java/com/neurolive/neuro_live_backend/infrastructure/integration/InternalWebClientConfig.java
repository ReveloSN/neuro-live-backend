package com.neurolive.neuro_live_backend.infrastructure.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// Configura el cliente interno hacia el WS Service.
@Configuration
public class InternalWebClientConfig {

    // Construye el WebClient con token interno compartido.
    @Bean
    public WebClient realtimeServiceClient(
            @Value("${realtime.service.base-url:http://localhost:8080}") String baseUrl,
            @Value("${internal.token:ws-internal-secret-change-in-prod}") String token) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", token)
                .build();
    }
}

