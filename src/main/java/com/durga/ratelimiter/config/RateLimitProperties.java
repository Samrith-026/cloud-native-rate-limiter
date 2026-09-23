package com.durga.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(long capacity, long windowSeconds, boolean trustForwardedFor) {
    public RateLimitProperties {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        if (windowSeconds < 1) throw new IllegalArgumentException("windowSeconds must be positive");
    }
}
