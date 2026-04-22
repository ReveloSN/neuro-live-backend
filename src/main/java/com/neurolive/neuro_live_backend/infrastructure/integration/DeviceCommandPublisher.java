package com.neurolive.neuro_live_backend.infrastructure.integration;

import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;

// Define el contrato para publicar comandos hacia el WS Service.
public interface DeviceCommandPublisher {

    // Publica un comando hacia el gateway realtime.
    void publish(DeviceCommand command);
}

