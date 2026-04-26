package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.properties.PersistenceRetryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class PersistenceRetryConfiguration {

    public static final String PERSISTENCE_RETRY_TEMPLATE = "persistenceRetryTemplate";

    @Bean(PERSISTENCE_RETRY_TEMPLATE)
    public RetryTemplate persistenceRetryTemplate(PersistenceRetryProperties properties) {
        return createPersistenceRetryTemplate(properties);
    }

    public static RetryTemplate createPersistenceRetryTemplate(PersistenceRetryProperties properties) {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                properties.maxAttempts(),
                Map.of(TransientDataAccessException.class, true),
                true);

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(properties.backoffMillis());

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOff);
        return template;
    }
}
