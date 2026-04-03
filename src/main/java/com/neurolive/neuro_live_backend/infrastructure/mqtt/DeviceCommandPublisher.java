package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;

// Define el contrato para publicar comandos hacia dispositivos externos.
public interface DeviceCommandPublisher {

    void publish(DeviceCommand command);
}
