package com.neurolive.neuro_live_backend.service;

import com.neurolive.neuro_live_backend.entity.DeviceCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpDeviceCommandPublisher implements DeviceCommandPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpDeviceCommandPublisher.class);

    @Override
    public void publish(DeviceCommand command) {
        LOGGER.info("Dispatching intervention command '{}' to device {}", command.command(), command.macAddress());
    }
}
