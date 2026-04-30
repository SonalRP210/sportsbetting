package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.domain.event.OddsUpdatedPayload;

public interface OddsPublisher {
    void publish(OddsUpdatedPayload payload);
}
