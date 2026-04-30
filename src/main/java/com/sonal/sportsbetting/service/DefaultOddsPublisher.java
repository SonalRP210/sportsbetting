package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;
import com.sonal.sportsbetting.model.OddsUpdate;
import org.springframework.stereotype.Component;

@Component
public class DefaultOddsPublisher implements OddsPublisher {

    private final OddsUpdateBroadcaster oddsUpdateBroadcaster;

    public DefaultOddsPublisher(OddsUpdateBroadcaster oddsUpdateBroadcaster) {
        this.oddsUpdateBroadcaster = oddsUpdateBroadcaster;
    }

    @Override
    public void publish(OddsUpdatedPayload payload) {
        OddsUpdate update = new OddsUpdate();
        update.setEventId(payload.eventId());
        update.setSelection(payload.selection());
        update.setOdds(payload.odds());
        oddsUpdateBroadcaster.broadcast(update);
    }
}
