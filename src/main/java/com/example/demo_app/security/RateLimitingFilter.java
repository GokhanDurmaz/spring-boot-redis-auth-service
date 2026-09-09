package com.example.demo_app.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final LettuceBasedProxyManager<String> proxyManager;

    public RateLimitingFilter(LettuceBasedProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    private BucketConfiguration createBucketConfiguration() {
        Bandwidth limit = Bandwidth.builder()
                            .capacity(10)
                            .refillIntervallyAligned(10, Duration.ofMinutes(1), Instant.now())
                            .build();
        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            String clientIp = getClientIP(request);

            // key_name on Redis: rate_limit:<path>:192.168.1.10
            String redisKey = "rate_limit:" + path + ":" + clientIp;

            var bucket = proxyManager.builder().build(redisKey, this::createBucketConfiguration);

            System.out.println(">>> [REDIS RATE LIMIT] IP: " + clientIp + " | Remaining Token: " + bucket.getAvailableTokens());

            // Try to consume from the bucket
            if (!bucket.tryConsume(1)) {
                System.out.println(">>> [REDIS RATE LIMIT EXCEEDED] IP: " + clientIp + " - 429 RETURNING");
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many request. Please try to request 1 minute after.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Nginx / Catching the real IP address behind the Ingress
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}