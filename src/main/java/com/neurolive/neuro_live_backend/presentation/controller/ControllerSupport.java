package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.infrastructure.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;

// Utilidades pequeñas compartidas entre controladores REST para evitar duplicar logica trivial.
final class ControllerSupport {

    private ControllerSupport() {
    }

    // Resuelve la IP del solicitante o devuelve "unknown" si la request no esta disponible.
    static String resolveIp(HttpServletRequest httpServletRequest) {
        return ClientIpResolver.resolve(httpServletRequest);
    }
}
