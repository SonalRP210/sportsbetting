package com.sonal.sportsbetting.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public record RedisProperties(
        boolean enabled,
        String rateLimitKeyPrefix,
        String oddsChannel
) {
}
