package com.sonal.sportsbetting.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.odds")
public record OddsProperties(
        String compositeKeySeparator
) {
}
