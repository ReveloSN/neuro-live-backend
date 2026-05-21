package com.neurolive.neuro_live_backend.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;

// Utilidades pequeñas compartidas entre controladores REST para evitar duplicar logica trivial.
final class ControllerSupport {

    private ControllerSupport() {
    }

    // Resuelve la IP del solicitante o devuelve "unknown" si la request no esta disponible.
    static String resolveIp(HttpServletRequest httpServletRequest) {
        if (httpServletRequest == null) {
            return "unknown";
        }
        String remoteAddress = httpServletRequest.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }
}
