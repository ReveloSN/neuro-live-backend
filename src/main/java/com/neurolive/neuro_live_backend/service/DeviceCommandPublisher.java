package com.neurolive.neuro_live_backend.service;

import com.neurolive.neuro_live_backend.entity.DeviceCommand;

public interface DeviceCommandPublisher {

    void publish(DeviceCommand command);
}
