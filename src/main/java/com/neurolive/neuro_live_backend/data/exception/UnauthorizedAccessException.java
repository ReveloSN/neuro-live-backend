package com.neurolive.neuro_live_backend.data.exception;

// Excepcion que indica un intento de acceso sin permisos suficientes.
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
