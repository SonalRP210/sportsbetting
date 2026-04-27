package com.sonal.sportsbetting.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterGatewayTest {

    @Test
    void allowsUpToMaxRequestsPerWindowThenRejects() {
        InMemoryRateLimiterGateway gateway = new InMemoryRateLimiterGateway();
        String key = "127.0.0.1:/api/v1/bets";
        long windowSeconds = 60;
        int maxRequests = 2;

        assertTrue(gateway.tryConsume(key, windowSeconds, maxRequests));
        assertTrue(gateway.tryConsume(key, windowSeconds, maxRequests));
        assertFalse(gateway.tryConsume(key, windowSeconds, maxRequests));
    }

    @Test
    void separateClientKeysAreTrackedIndependently() {
        InMemoryRateLimiterGateway gateway = new InMemoryRateLimiterGateway();

        assertTrue(gateway.tryConsume("a:/x", 60, 1));
        assertFalse(gateway.tryConsume("a:/x", 60, 1));

        assertTrue(gateway.tryConsume("b:/x", 60, 1));
    }
}
