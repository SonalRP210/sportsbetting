package com.sonal.sportsbetting.service.outbox;

import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;
import com.sonal.sportsbetting.properties.OddsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLatestOddsHashWriter {
    private static final Logger log = LoggerFactory.getLogger(RedisLatestOddsHashWriter.class);
    private static final String HASH_KEY = "odds:latest";

    private final boolean redisEnabled;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final OddsProperties oddsProperties;

    public RedisLatestOddsHashWriter(
            @Value("${app.redis.enabled:false}") boolean redisEnabled,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            OddsProperties oddsProperties) {
        this.redisEnabled = redisEnabled;
        this.redisTemplateProvider = redisTemplateProvider;
        this.oddsProperties = oddsProperties;
    }

    public void writeIfEnabled(OddsUpdatedPayload payload) {
        if (!redisEnabled) {
            return;
        }
        StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
        if (template == null) {
            log.warn("Redis enabled but StringRedisTemplate unavailable; skipping odds hash write");
            return;
        }
        String field = payload.eventId() + oddsProperties.compositeKeySeparator() + payload.selection();
        template.opsForHash().put(HASH_KEY, field, payload.odds().toPlainString());
    }
}
