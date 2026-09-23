package com.durga.ratelimiter.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisConfig {
    @Bean
    public DefaultRedisScript<List> rateLimitScript() {
        var script = new DefaultRedisScript<List>();
        script.setLocation(new org.springframework.core.io.ClassPathResource("rate_limit.lua"));
        script.setResultType(List.class);
        return script;
    }
}
