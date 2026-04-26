package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.PropertyFixtures;
import com.sonal.sportsbetting.properties.CorrelationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CorrelationTestSupport {

    @Bean
    public CorrelationProperties correlationProperties() {
        return PropertyFixtures.correlation();
    }
}
