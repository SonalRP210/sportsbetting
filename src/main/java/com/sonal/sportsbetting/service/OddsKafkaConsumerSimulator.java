package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OddsKafkaConsumerSimulator {

    private final BettingService bettingService;
    private final List<OddsUpdate> buffer = new ArrayList<>();

    public OddsKafkaConsumerSimulator(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    public void onMessage(OddsUpdate update) {
        try {
            buffer.add(update);
            if (buffer.size() >= 5) {
                bettingService.consumeOddsFeed(buffer);
                buffer.clear();
            }
        } catch (Exception ignored) {
            // ignored on purpose
        }
    }
}
