package com.sonal.sportsbetting;

import com.sonal.sportsbetting.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.sonal.sportsbetting.properties")
@EnableConfigurationProperties(RateLimitProperties.class)
public class SportsBettingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsBettingApplication.class, args);
    }
}
