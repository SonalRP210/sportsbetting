package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.service.DefaultOddsPublisher;
import com.sonal.sportsbetting.service.OddsUpdateBroadcaster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultOddsPublisherTest {

    @Mock
    private OddsUpdateBroadcaster oddsUpdateBroadcaster;

    @Test
    void publishConvertsPayloadAndBroadcasts() {
        DefaultOddsPublisher publisher = new DefaultOddsPublisher(oddsUpdateBroadcaster);
        OddsUpdatedPayload payload = new OddsUpdatedPayload("evt", "home", new BigDecimal("1.90"));

        publisher.publish(payload);

        ArgumentCaptor<OddsUpdate> captor = ArgumentCaptor.forClass(OddsUpdate.class);
        verify(oddsUpdateBroadcaster).broadcast(captor.capture());
        assertEquals("evt", captor.getValue().getEventId());
        assertEquals("home", captor.getValue().getSelection());
        assertEquals(new BigDecimal("1.90"), captor.getValue().getOdds());
    }
}
