package com.sonal.sportsbetting.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.correlation")
public record CorrelationProperties(
        String requestIdHeader,
        String responseIdHeader,
        String mdcKey
) {
}
