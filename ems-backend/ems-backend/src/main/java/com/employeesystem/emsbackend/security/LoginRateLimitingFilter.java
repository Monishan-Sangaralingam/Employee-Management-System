package com.employeesystem.emsbackend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LoginRateLimitingFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";

    private final Map<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    @Value("${LOGIN_RATE_LIMIT_ATTEMPTS:10}")
    private long attempts;

    @Value("${LOGIN_RATE_LIMIT_WINDOW_MINUTES:10}")
    private long windowMinutes;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        return uri == null || !uri.equals(LOGIN_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);

        Bucket bucket = bucketsByIp.computeIfAbsent(clientIp, ip -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"Too many login attempts. Please try again later.\"}");
        response.getWriter().flush();
    }

    private Bucket newBucket() {
        long configuredAttempts = attempts > 0 ? attempts : 10;
        long configuredWindowMinutes = windowMinutes > 0 ? windowMinutes : 10;

        Bandwidth limit = Bandwidth.classic(
                configuredAttempts,
                Refill.intervally(configuredAttempts, Duration.ofMinutes(configuredWindowMinutes))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (!first.isBlank()) {
                return first;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr;
    }
}
