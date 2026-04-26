package com.sonal.sportsbetting.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retry.persistence")
public record PersistenceRetryProperties(
        int maxAttempts,
        long backoffMillis
) {
}
