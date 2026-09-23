package com.durga.ratelimiter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.durga.ratelimiter.config.RateLimitProperties;
import com.durga.ratelimiter.core.RateLimitDecision;
import com.durga.ratelimiter.core.RateLimitService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {
    @Test
    void rejectedRequestReturnsDelayBasedHeaders() throws Exception {
        RateLimitService service = mock(RateLimitService.class);
        when(service.check(anyString())).thenReturn(
                new RateLimitDecision(false, 20, 0, Instant.now().getEpochSecond() + 30));
        var filter = new RateLimitFilter(
                service, new SimpleMeterRegistry(), new RateLimitProperties(20, 60, false));
        var request = new MockHttpServletRequest("GET", "/api/demo");
        request.addHeader("X-API-Key", "secret-demo-key");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("RateLimit-Limit")).isEqualTo("20");
        assertThat(response.getHeader("RateLimit-Remaining")).isEqualTo("0");
        assertThat(Long.parseLong(response.getHeader("RateLimit-Reset"))).isBetween(1L, 30L);
        assertThat(response.getHeader("Retry-After")).isEqualTo(response.getHeader("RateLimit-Reset"));
        assertThat(response.getContentAsString()).contains("rate_limit_exceeded");
    }
}
