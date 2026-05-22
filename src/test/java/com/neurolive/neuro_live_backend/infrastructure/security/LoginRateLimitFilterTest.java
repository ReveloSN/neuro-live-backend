package com.neurolive.neuro_live_backend.infrastructure.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Verifica el limite de intentos de login por IP real.
class LoginRateLimitFilterTest {

    private final LoginRateLimitFilter filter = new LoginRateLimitFilter();

    @Test
    void shouldRejectEleventhLoginAttemptForSameForwardedIp() throws ServletException, IOException {
        for (int attempt = 1; attempt <= 10; attempt++) {
            MockHttpServletResponse response = executeLoginAttempt("203.0.113.10");
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse response = executeLoginAttempt("203.0.113.10");

        assertEquals(429, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
    }

    @Test
    void shouldNotLimitNonLoginRequests() throws ServletException, IOException {
        for (int attempt = 1; attempt <= 11; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
            request.addHeader("X-Forwarded-For", "203.0.113.11");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(200, response.getStatus());
        }
    }

    private MockHttpServletResponse executeLoginAttempt(String ip) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("X-Forwarded-For", ip + ", 10.0.0.5");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        return response;
    }
}
