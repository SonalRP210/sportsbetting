package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "odds-enabled", havingValue = "true")
public class KafkaOddsFeedConsumer {

    private final OddsService oddsService;

    public KafkaOddsFeedConsumer(OddsService oddsService) {
        this.oddsService = oddsService;
    }

    @KafkaListener(
            topics = "${app.kafka.odds-topic}",
            groupId = "${app.kafka.odds-group-id}")
    public void consume(OddsUpdate update) {
        oddsService.consumeOddsFeed(List.of(update));
    }
}
