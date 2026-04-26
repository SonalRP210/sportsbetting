package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.response.SettleEventResponse;

public interface SettlementService {
    SettleEventResponse settleEvent(String eventId, String winningSelection);
}
