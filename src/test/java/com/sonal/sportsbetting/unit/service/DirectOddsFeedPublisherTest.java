package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.service.DirectOddsFeedPublisher;
import com.sonal.sportsbetting.service.OddsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DirectOddsFeedPublisherTest {

    @Mock
    private OddsService oddsService;

    @Test
    void publishDelegatesToOddsService() {
        DirectOddsFeedPublisher publisher = new DirectOddsFeedPublisher(oddsService);
        List<OddsUpdate> updates = List.of(new OddsUpdate());

        publisher.publish(updates);

        verify(oddsService).consumeOddsFeed(updates);
    }
}
