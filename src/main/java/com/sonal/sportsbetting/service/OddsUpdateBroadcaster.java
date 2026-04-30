package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.OddsUpdate;

public interface OddsUpdateBroadcaster {
    void broadcast(OddsUpdate update);
}
