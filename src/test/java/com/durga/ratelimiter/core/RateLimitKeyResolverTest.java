package com.durga.ratelimiter.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class RateLimitKeyResolverTest {
    @Test
    void prefersApiKey() {
        var request = mock(HttpServletRequest.class);
        when(request.getHeader("X-API-Key")).thenReturn("customer 42");
        String resolved = RateLimitKeyResolver.resolve(request, false);
        assertThat(resolved).startsWith("key:").hasSize(36).doesNotContain("customer");
        assertThat(resolved).isEqualTo(RateLimitKeyResolver.resolve(request, false));
    }

    @Test
    void ignoresForwardedAddressByDefault() {
        var request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.7, 10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("192.0.2.8");
        assertThat(RateLimitKeyResolver.resolve(request, false))
                .isNotEqualTo(RateLimitKeyResolver.resolve(request, true));
    }

    @Test
    void trustsFirstForwardedAddressOnlyWhenConfigured() {
        var forwarded = mock(HttpServletRequest.class);
        when(forwarded.getHeader("X-Forwarded-For")).thenReturn("203.0.113.7, 10.0.0.1");

        var direct = mock(HttpServletRequest.class);
        when(direct.getRemoteAddr()).thenReturn("203.0.113.7");

        assertThat(RateLimitKeyResolver.resolve(forwarded, true))
                .isEqualTo(RateLimitKeyResolver.resolve(direct, false));
    }
}
