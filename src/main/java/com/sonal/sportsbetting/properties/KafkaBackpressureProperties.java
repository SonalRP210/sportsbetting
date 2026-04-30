package com.sonal.sportsbetting.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaBackpressureProperties(
        boolean oddsEnabled,
        String oddsTopic,
        String oddsGroupId
) {
}
