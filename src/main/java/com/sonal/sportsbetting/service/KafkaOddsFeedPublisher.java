package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.properties.KafkaBackpressureProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "odds-enabled", havingValue = "true")
public class KafkaOddsFeedPublisher implements OddsFeedPublisher {

    private final KafkaTemplate<String, OddsUpdate> kafkaTemplate;
    private final KafkaBackpressureProperties kafkaProperties;

    public KafkaOddsFeedPublisher(
            KafkaTemplate<String, OddsUpdate> kafkaTemplate,
            KafkaBackpressureProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public void publish(List<OddsUpdate> feedEvents) {
        for (OddsUpdate update : feedEvents) {
            kafkaTemplate.send(kafkaProperties.oddsTopic(), update.getEventId(), update);
        }
    }
}
