package com.sonal.sportsbetting.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(
        int defaultPage,
        int defaultPageSize
) {
}
