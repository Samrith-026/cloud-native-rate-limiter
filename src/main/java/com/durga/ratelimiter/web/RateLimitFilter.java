package com.durga.ratelimiter.web;

import com.durga.ratelimiter.config.RateLimitProperties;
import com.durga.ratelimiter.core.RateLimitDecision;
import com.durga.ratelimiter.core.RateLimitKeyResolver;
import com.durga.ratelimiter.core.RateLimitService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private final RateLimitService service;
    private final Counter accepted;
    private final Counter rejected;
    private final boolean trustForwardedFor;

    public RateLimitFilter(RateLimitService service, MeterRegistry registry, RateLimitProperties properties) {
        this.service = service;
        this.accepted = Counter.builder("rate_limit_decisions_total").tag("decision", "accepted").register(registry);
        this.rejected = Counter.builder("rate_limit_decisions_total").tag("decision", "rejected").register(registry);
        this.trustForwardedFor = properties.trustForwardedFor();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String identity = RateLimitKeyResolver.resolve(request, trustForwardedFor);
        try {
            RateLimitDecision decision = service.check(identity);
            long resetAfterSeconds = Math.max(1, decision.resetEpochSeconds() - Instant.now().getEpochSecond());
            setHeaders(response, decision, resetAfterSeconds);
            if (!decision.allowed()) {
                rejected.increment();
                response.setStatus(429);
                response.setContentType("application/json");
                response.setHeader("Retry-After", String.valueOf(resetAfterSeconds));
                response.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
                log.warn("rate_limit_rejected identity={} path={}", identity, request.getRequestURI());
                return;
            }
            accepted.increment();
            chain.doFilter(request, response);
        } catch (RuntimeException unavailable) {
            response.setStatus(503);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limit_service_unavailable\"}");
            log.error("rate_limit_backend_failure path={}", request.getRequestURI(), unavailable);
        }
    }

    private static void setHeaders(HttpServletResponse response, RateLimitDecision decision, long resetAfterSeconds) {
        response.setHeader("RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("RateLimit-Reset", String.valueOf(resetAfterSeconds));
    }
}
