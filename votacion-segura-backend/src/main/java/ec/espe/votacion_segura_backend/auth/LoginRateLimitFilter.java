package ec.espe.votacion_segura_backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight per-IP login throttle for a single application instance.
 * A distributed deployment should enforce the same limit at the gateway.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final int limit;
    private final Duration window;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(
            @Value("${app.security.login-rate-limit:10}") int limit,
            @Value("${app.security.login-rate-window-seconds:60}") long windowSeconds) {
        this.limit = Math.max(1, limit);
        this.window = Duration.ofSeconds(Math.max(1, windowSeconds));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/v1/auth/login".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr();
        Instant now = Instant.now();
        Counter counter = counters.compute(key, (ignored, current) -> {
            if (current == null || current.started.plus(window).isBefore(now)) return new Counter(now, 1);
            return new Counter(current.started, current.requests + 1);
        });
        if (counter.requests > limit) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(Math.max(1, window.minus(Duration.between(counter.started, now)).toSeconds())));
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"error\":\"Demasiadas solicitudes\",\"message\":\"Intenta nuevamente más tarde\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private record Counter(Instant started, int requests) { }
}
