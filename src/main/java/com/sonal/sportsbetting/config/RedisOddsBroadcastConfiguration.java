package com.sonal.sportsbetting.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.properties.RedisProperties;
import com.sonal.sportsbetting.service.DefaultOddsService;
import com.sonal.sportsbetting.service.OddsUpdateBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisOddsBroadcastConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RedisOddsBroadcastConfiguration.class);

    @Bean
    public OddsUpdateBroadcaster redisOddsUpdateBroadcaster(
            StringRedisTemplate redisTemplate,
            RedisProperties redisProperties,
            ObjectMapper objectMapper) {
        return update -> {
            try {
                redisTemplate.convertAndSend(redisProperties.oddsChannel(), objectMapper.writeValueAsString(update));
            } catch (JsonProcessingException ex) {
                log.warn("Failed to serialize odds update eventId={} selection={}",
                        update.getEventId(), update.getSelection(), ex);
            }
        };
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisProperties redisProperties,
            ObjectMapper objectMapper,
            DefaultOddsService defaultOddsService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new OddsMessageListener(objectMapper, defaultOddsService),
                new ChannelTopic(redisProperties.oddsChannel()));
        return container;
    }

    private static class OddsMessageListener implements MessageListener {
        private final ObjectMapper objectMapper;
        private final DefaultOddsService defaultOddsService;

        private OddsMessageListener(ObjectMapper objectMapper, DefaultOddsService defaultOddsService) {
            this.objectMapper = objectMapper;
            this.defaultOddsService = defaultOddsService;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                OddsUpdate update = objectMapper.readValue(message.getBody(), OddsUpdate.class);
                defaultOddsService.applyBroadcastUpdate(update);
            } catch (Exception ex) {
                log.warn("Failed to process odds pub/sub message", ex);
            }
        }
    }
}
