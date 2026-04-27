package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.properties.RedisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisRateLimiterGateway implements RateLimiterGateway {
    private static final String LUA = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
              return 0
            end
            return 1
            """;

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties redisProperties;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RedisRateLimiterGateway(StringRedisTemplate redisTemplate, RedisProperties redisProperties) {
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
        this.rateLimitScript = new DefaultRedisScript<>(LUA, Long.class);
    }

    @Override
    public boolean tryConsume(String clientKey, long windowSeconds, int maxRequests) {
        String key = redisProperties.rateLimitKeyPrefix() + clientKey;
        Long result = redisTemplate.execute(
                rateLimitScript,
                List.of(key),
                String.valueOf(windowSeconds),
                String.valueOf(maxRequests));
        return Long.valueOf(1L).equals(result);
    }
}
