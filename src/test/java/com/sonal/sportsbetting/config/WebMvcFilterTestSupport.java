package com.sonal.sportsbetting.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Supplies beans required by servlet filters in {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}
 * slices, where full component scanning is not active.
 */
@TestConfiguration
public class WebMvcFilterTestSupport {

    @Bean
    public RateLimiterGateway rateLimiterGateway() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRequests(1000);
        properties.setWindowSeconds(60);
        return new InMemoryRateLimiterGateway(properties);
    }
}
