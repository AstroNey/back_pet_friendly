package lns.back.backend_pet_friendly.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting simple par IP sur les endpoints d'authentification (login/register/refresh/logout),
 * en fenêtre fixe. Anti-brute-force / anti-énumération basique. Compteur en mémoire : suffisant pour
 * un déploiement mono-instance derrière Caddy ; passer à un store partagé (Redis) si scale horizontal.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMs;
    private final int maxTrackedKeys;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(int maxRequests, long windowMs, int maxTrackedKeys) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.maxTrackedKeys = maxTrackedKeys;
    }

    private static final class Window {
        long start;
        int count;
        Window(long start, int count) { this.start = start; this.count = count; }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        if (windows.size() > maxTrackedKeys) {
            windows.values().removeIf(w -> now - w.start >= windowMs);
        }
        Window w = windows.compute(clientIp(req), (k, cur) -> {
            if (cur == null || now - cur.start >= windowMs) return new Window(now, 1);
            cur.count++;
            return cur;
        });
        if (w.count > maxRequests) {
            long retryAfter = Math.max(1, (windowMs - (now - w.start)) / 1000);
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(retryAfter));
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":429,\"error\":\"Too many requests\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return req.getRemoteAddr();
    }
}
