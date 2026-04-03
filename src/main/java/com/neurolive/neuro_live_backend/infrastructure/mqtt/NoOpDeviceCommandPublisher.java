package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Implementacion temporal que solo registra el envio de comandos.
public class NoOpDeviceCommandPublisher implements DeviceCommandPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpDeviceCommandPublisher.class);

    @Override
    public void publish(DeviceCommand command) {
        LOGGER.info("Dispatching intervention command '{}' to device {}", command.command(), command.macAddress());
    }
}
