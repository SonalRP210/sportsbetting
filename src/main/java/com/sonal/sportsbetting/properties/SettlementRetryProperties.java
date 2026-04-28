package com.sonal.sportsbetting.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retry.settlement")
public record SettlementRetryProperties(
        int maxAttempts,
        long backoffMillis
) {
}
