package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.PropertyFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersistenceRetryConfigurationTest {

    @Test
    void retriesTransientDataAccessFailures() {
        RetryTemplate template = PersistenceRetryConfiguration.createPersistenceRetryTemplate(
                PropertyFixtures.persistenceRetry());
        AtomicInteger attempts = new AtomicInteger();

        String result = template.execute(new RetryCallback<String, RuntimeException>() {
            @Override
            public String doWithRetry(RetryContext context) {
                int n = attempts.incrementAndGet();
                if (n < 3) {
                    throw new TransientDataAccessResourceException("simulated flake");
                }
                return "ok";
            }
        });

        assertEquals("ok", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void doesNotRetryNonTransientExceptions() {
        RetryTemplate template = PersistenceRetryConfiguration.createPersistenceRetryTemplate(
                PropertyFixtures.persistenceRetry());
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> template.execute(new RetryCallback<String, RuntimeException>() {
            @Override
            public String doWithRetry(RetryContext context) {
                attempts.incrementAndGet();
                throw new IllegalStateException("not transient");
            }
        }));

        assertEquals(1, attempts.get());
    }
}
