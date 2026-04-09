package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.data.exception.CrisisNotFoundException;
import com.neurolive.neuro_live_backend.data.exception.DeviceNotLinkedException;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
// Expone errores de aplicacion en formato JSON consistente para el frontend.
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String message = fieldError != null && fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Request validation failed";

        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CrisisNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCrisisNotFound(CrisisNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({UnauthorizedAccessException.class, DeviceNotLinkedException.class})
    public ResponseEntity<Map<String, String>> handleForbidden(RuntimeException exception) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> build(HttpStatus status, String message) {
        String normalizedMessage = message == null || message.isBlank()
                ? "Request could not be processed"
                : message;

        return ResponseEntity.status(status).body(Map.of("message", normalizedMessage));
    }
}
