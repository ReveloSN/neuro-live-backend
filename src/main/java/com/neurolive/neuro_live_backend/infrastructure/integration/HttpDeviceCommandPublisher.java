package com.neurolive.neuro_live_backend.infrastructure.integration;

import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// Publica comandos al WS Service que luego los entrega al ESP32.
@Component
public class HttpDeviceCommandPublisher implements DeviceCommandPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpDeviceCommandPublisher.class);

    private final WebClient realtimeServiceClient;

    // Recibe el cliente HTTP interno del gateway realtime.
    public HttpDeviceCommandPublisher(WebClient realtimeServiceClient) {
        this.realtimeServiceClient = realtimeServiceClient;
    }

    // Publica el comando hacia el servicio realtime.
    @Override
    public void publish(DeviceCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Device command is required");
        }
        try {
            realtimeServiceClient.post()
                    .uri("/api/devices/{id}/commands/light", command.macAddress())
                    .bodyValue(buildLightCommandRequest(command))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(3));
        } catch (Exception exception) {
            LOGGER.error("Failed to send command to device {}: {}", command.macAddress(), exception.getMessage());
            throw new IllegalStateException("Command delivery failed for device " + command.macAddress(), exception);
        }
    }

    // Adapta el comando clinico al payload de luz esperado por el WS Service.
    private Map<String, Object> buildLightCommandRequest(DeviceCommand command) {
        String normalizedCommand = command.command() == null
                ? "CALM_MODE"
                : command.command().trim().toUpperCase();
        return Map.of(
                "color", resolveColor(normalizedCommand),
                "intensity", resolveIntensity(normalizedCommand),
                "mode", resolveMode(normalizedCommand)
        );
    }

    // Resuelve un color simple segun el tipo de comando.
    private String resolveColor(String normalizedCommand) {
        return switch (normalizedCommand) {
            case "LIGHT_INTERVENTION" -> "#4A90E2";
            case "BREATHING_INTERVENTION" -> "#7BCFA6";
            case "AUDIO_INTERVENTION" -> "#FFD166";
            case "UI_INTERVENTION" -> "#A7C7E7";
            default -> "#4A90E2";
        };
    }

    // Resuelve una intensidad razonable segun el comando.
    private int resolveIntensity(String normalizedCommand) {
        return switch (normalizedCommand) {
            case "LIGHT_INTERVENTION" -> 80;
            case "AUDIO_INTERVENTION" -> 65;
            default -> 60;
        };
    }

    // Resuelve un modo compatible con el WS Service.
    private String resolveMode(String normalizedCommand) {
        return switch (normalizedCommand) {
            case "LIGHT_INTERVENTION" -> "pulse";
            case "AUDIO_INTERVENTION", "UI_INTERVENTION" -> "steady";
            default -> "calm";
        };
    }
}

