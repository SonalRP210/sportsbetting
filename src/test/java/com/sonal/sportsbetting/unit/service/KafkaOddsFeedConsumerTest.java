package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.service.KafkaOddsFeedConsumer;
import com.sonal.sportsbetting.service.OddsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaOddsFeedConsumerTest {

    @Mock
    private OddsService oddsService;

    @Test
    void consumeDelegatesSingleEventToOddsService() {
        KafkaOddsFeedConsumer consumer = new KafkaOddsFeedConsumer(oddsService);
        OddsUpdate update = new OddsUpdate();
        update.setEventId("e1");
        update.setSelection("home");

        consumer.consume(update);

        verify(oddsService).consumeOddsFeed(List.of(update));
    }
}
