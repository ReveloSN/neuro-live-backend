package com.neurolive.neuro_live_backend.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

// Centraliza la resolucion de IP real para auditoria y controles de seguridad.
public final class ClientIpResolver {

    private static final String UNKNOWN = "unknown";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String forwardedFor = firstForwardedFor(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }
        String realIp = clean(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        String forwarded = forwardedHeaderAddress(request.getHeader("Forwarded"));
        if (forwarded != null) {
            return forwarded;
        }
        String remoteAddress = clean(request.getRemoteAddr());
        return remoteAddress == null ? UNKNOWN : remoteAddress;
    }

    private static String firstForwardedFor(String header) {
        String value = clean(header);
        if (value == null) {
            return null;
        }
        for (String candidate : value.split(",")) {
            String ip = clean(candidate);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    private static String forwardedHeaderAddress(String header) {
        String value = clean(header);
        if (value == null) {
            return null;
        }
        String firstForwardedValue = value.split(",", 2)[0];
        for (String part : firstForwardedValue.split(";")) {
            String trimmed = part.trim();
            if (trimmed.regionMatches(true, 0, "for=", 0, 4)) {
                return clean(trimmed.substring(4));
            }
        }
        return null;
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned.isBlank() ? null : cleaned;
    }
}
