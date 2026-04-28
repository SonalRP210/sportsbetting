package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.properties.RedisProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterGatewayTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void tryConsumeTrueWhenLuaReturnsOne() {
        when(redisTemplate.execute(any(), anyList(), any(), any()))
                .thenReturn(1L);
        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setWindowSeconds(60);
        rateLimitProperties.setRequests(120);

        RedisRateLimiterGateway gateway = new RedisRateLimiterGateway(
                redisTemplate,
                new RedisProperties(true, "rate_limit:", "odds:updates"),
                rateLimitProperties);

        assertTrue(gateway.tryConsume("127.0.0.1:/api/v1/health"));
    }

    @Test
    void tryConsumeFalseWhenLuaReturnsZero() {
        when(redisTemplate.execute(any(), anyList(), any(), any()))
                .thenReturn(0L);
        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setWindowSeconds(60);
        rateLimitProperties.setRequests(120);

        RedisRateLimiterGateway gateway = new RedisRateLimiterGateway(
                redisTemplate,
                new RedisProperties(true, "rate_limit:", "odds:updates"),
                rateLimitProperties);

        assertFalse(gateway.tryConsume("127.0.0.1:/api/v1/health"));
    }

    @Test
    void prefixesClientKeyWithConfiguredPrefix() {
        when(redisTemplate.execute(any(), anyList(), any(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<String> keys = (List<String>) invocation.getArgument(1);
                    assertTrue(keys.getFirst().startsWith("rate_limit:"));
                    assertTrue(keys.getFirst().contains("client-key"));
                    return 1L;
                });
        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setWindowSeconds(30);
        rateLimitProperties.setRequests(5);

        RedisRateLimiterGateway gateway = new RedisRateLimiterGateway(
                redisTemplate,
                new RedisProperties(true, "rate_limit:", "odds:updates"),
                rateLimitProperties);

        assertTrue(gateway.tryConsume("client-key"));
    }
}
