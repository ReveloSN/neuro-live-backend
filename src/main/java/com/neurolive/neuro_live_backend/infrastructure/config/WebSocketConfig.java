package com.neurolive.neuro_live_backend.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
// Habilita un broker STOMP simple para dashboards clinicos en tiempo real.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;

    public WebSocketConfig(@Value("${app.allowed-origins:http://localhost:3000,http://localhost:5173,https://neuro-live-frontend.vercel.app}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null
                ? new String[]{"http://localhost:3000", "http://localhost:5173", "https://neuro-live-frontend.vercel.app"}
                : allowedOrigins.split(",");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/patient-state")
                .setAllowedOriginPatterns(normalizeAllowedOrigins());
    }

    private String[] normalizeAllowedOrigins() {
        return java.util.Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }
}
