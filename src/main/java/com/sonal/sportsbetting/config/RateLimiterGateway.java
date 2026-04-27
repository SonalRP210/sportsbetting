package com.sonal.sportsbetting.config;

public interface RateLimiterGateway {
    boolean tryConsume(String clientKey, long windowSeconds, int maxRequests);
}
