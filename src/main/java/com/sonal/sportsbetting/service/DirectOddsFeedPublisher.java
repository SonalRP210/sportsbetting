package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "odds-enabled", havingValue = "false", matchIfMissing = true)
public class DirectOddsFeedPublisher implements OddsFeedPublisher {

    private final OddsService oddsService;

    public DirectOddsFeedPublisher(OddsService oddsService) {
        this.oddsService = oddsService;
    }

    @Override
    public void publish(List<OddsUpdate> feedEvents) {
        oddsService.consumeOddsFeed(feedEvents);
    }
}
