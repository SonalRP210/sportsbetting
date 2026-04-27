package com.sonal.sportsbetting.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimiterGateway implements RateLimiterGateway {
    private final ConcurrentHashMap<String, ClientWindow> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String clientKey, long windowSeconds, int maxRequests) {
        long nowEpochSecond = Instant.now().getEpochSecond();
        ClientWindow window = windows.compute(clientKey, (key, existing) -> {
            if (existing == null || nowEpochSecond - existing.windowStartEpochSecond() >= windowSeconds) {
                return new ClientWindow(nowEpochSecond, new AtomicInteger(1));
            }
            existing.counter().incrementAndGet();
            return existing;
        });
        return window.counter().get() <= maxRequests;
    }

    private record ClientWindow(long windowStartEpochSecond, AtomicInteger counter) {
    }
}
