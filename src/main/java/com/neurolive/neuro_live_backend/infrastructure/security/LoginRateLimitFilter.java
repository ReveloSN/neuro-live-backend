package com.neurolive.neuro_live_backend.infrastructure.security;

import com.neurolive.neuro_live_backend.infrastructure.web.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
// Limita intentos de login por IP para mitigar ataques de fuerza bruta.
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, AttemptBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/auth/login".equals(request.getRequestURI())) {
            String ip = ClientIpResolver.resolve(request);
            if (isRateLimited(ip)) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Too Many Requests\"," +
                        "\"message\":\"Demasiados intentos de inicio de sesión. Intente de nuevo en 1 minuto.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        AttemptBucket bucket = buckets.compute(ip, (k, v) -> v == null
                ? new AttemptBucket(1, now)
                : v.increment(now));
        return bucket.exceedsLimit();
    }

    private record AttemptBucket(long attempts, long windowStartedAt) {

        AttemptBucket increment(long now) {
            if (now - windowStartedAt > WINDOW_MS) {
                return new AttemptBucket(1, now);
            }
            return new AttemptBucket(attempts + 1, windowStartedAt);
        }

        boolean exceedsLimit() {
            return attempts > MAX_ATTEMPTS;
        }
    }
}
