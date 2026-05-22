package com.neurolive.neuro_live_backend.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Verifica la resolucion de IP real cuando la app esta detras de proxies.
class ClientIpResolverTest {

    @Test
    void resolve_shouldPreferFirstForwardedForAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.2");
        request.setRemoteAddr("127.0.0.1");

        assertEquals("203.0.113.9", ClientIpResolver.resolve(request));
    }

    @Test
    void resolve_shouldUseForwardedHeaderWhenForwardedForIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Forwarded", "for=198.51.100.4;proto=https;host=neurolive.test");
        request.setRemoteAddr("127.0.0.1");

        assertEquals("198.51.100.4", ClientIpResolver.resolve(request));
    }

    @Test
    void resolve_shouldFallbackToRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");

        assertEquals("192.0.2.10", ClientIpResolver.resolve(request));
    }

    @Test
    void resolve_shouldReturnUnknownWhenRequestIsMissing() {
        assertEquals("unknown", ClientIpResolver.resolve(null));
    }
}
