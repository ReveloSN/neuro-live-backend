package com.neurolive.neuro_live_backend.data.exception;

// Excepcion que representa un fallo de autenticacion por credenciales invalidas.
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
