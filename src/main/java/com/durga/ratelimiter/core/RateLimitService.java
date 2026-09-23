package com.durga.ratelimiter.core;

import com.durga.ratelimiter.config.RateLimitProperties;
import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;
    private final RateLimitProperties properties;

    public RateLimitService(StringRedisTemplate redis, DefaultRedisScript<List> script, RateLimitProperties properties) {
        this.redis = redis;
        this.script = script;
        this.properties = properties;
    }

    public RateLimitDecision check(String identity) {
        long now = Instant.now().getEpochSecond();
        long window = properties.windowSeconds();
        long windowStart = now - (now % window);
        String key = "rate-limit:" + identity + ":" + windowStart;
        List<?> result = redis.execute(script, List.of(key), String.valueOf(properties.capacity()), String.valueOf(window));
        if (result == null || result.size() < 2) throw new IllegalStateException("Redis returned an invalid rate-limit result");
        boolean allowed = ((Number) result.get(0)).longValue() == 1;
        long remaining = ((Number) result.get(1)).longValue();
        return new RateLimitDecision(allowed, properties.capacity(), remaining, windowStart + window);
    }
}
