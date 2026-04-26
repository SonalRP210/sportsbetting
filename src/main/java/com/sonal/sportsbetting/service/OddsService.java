package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OddsService {
    void consumeOddsFeed(List<OddsUpdate> feedEvents);

    Optional<BigDecimal> getOdds(String eventId, String selection);
}
