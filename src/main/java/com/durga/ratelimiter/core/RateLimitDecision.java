package com.durga.ratelimiter.core;

public record RateLimitDecision(boolean allowed, long limit, long remaining, long resetEpochSeconds) {}
