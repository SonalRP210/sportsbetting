package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;

import java.util.List;

public interface OddsFeedPublisher {
    void publish(List<OddsUpdate> feedEvents);
}
