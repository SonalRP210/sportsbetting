package com.sonal.sportsbetting.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterGatewayTest {

    @Test
    void allowsUpToMaxRequestsPerWindowThenRejects() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setWindowSeconds(60);
        properties.setRequests(2);
        InMemoryRateLimiterGateway gateway = new InMemoryRateLimiterGateway(properties);
        String key = "127.0.0.1:/api/v1/bets";

        assertTrue(gateway.tryConsume(key));
        assertTrue(gateway.tryConsume(key));
        assertFalse(gateway.tryConsume(key));
    }

    @Test
    void separateClientKeysAreTrackedIndependently() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setWindowSeconds(60);
        properties.setRequests(1);
        InMemoryRateLimiterGateway gateway = new InMemoryRateLimiterGateway(properties);

        assertTrue(gateway.tryConsume("a:/x"));
        assertFalse(gateway.tryConsume("a:/x"));

        assertTrue(gateway.tryConsume("b:/x"));
    }
}
