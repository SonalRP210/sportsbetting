package com.sonal.sportsbetting.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.betting")
public record BettingProperties(
        String betIdPrefix,
        int moneyScale,
        String moneyRoundingMode,
        int idempotencyKeyMaxLength
) {
}
