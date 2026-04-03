package com.neurolive.neuro_live_backend.data.exception;

// Excepcion que indica que el dispositivo no esta vinculado al paciente esperado.
public class DeviceNotLinkedException extends RuntimeException {

    public DeviceNotLinkedException(String message) {
        super(message);
    }
}
