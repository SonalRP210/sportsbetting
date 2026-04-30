package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.properties.SettlementRetryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class SettlementRetryConfiguration {

    public static final String SETTLEMENT_RETRY_TEMPLATE = "settlementRetryTemplate";

    @Bean(SETTLEMENT_RETRY_TEMPLATE)
    public RetryTemplate settlementRetryTemplate(SettlementRetryProperties properties) {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                properties.maxAttempts(),
                Map.of(
                        CannotAcquireLockException.class, true,
                        PessimisticLockingFailureException.class, true,
                        QueryTimeoutException.class, true),
                true);

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(properties.backoffMillis());

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOff);
        return template;
    }
}
