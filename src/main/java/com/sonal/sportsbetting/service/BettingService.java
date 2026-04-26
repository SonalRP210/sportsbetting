package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.request.PlaceBetRequest;
import com.sonal.sportsbetting.dto.response.BetDetailResponse;
import com.sonal.sportsbetting.dto.response.CancelBetResponse;
import com.sonal.sportsbetting.dto.response.PlaceBetResponse;
import com.sonal.sportsbetting.dto.response.SettleEventResponse;
import com.sonal.sportsbetting.dto.response.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.response.UserExposureResponse;
import com.sonal.sportsbetting.model.OddsUpdate;

import java.math.BigDecimal;
import java.util.List;

public interface BettingService {

    default PlaceBetResponse placeBet(PlaceBetRequest request) {
        return placeBet(request, null);
    }

    PlaceBetResponse placeBet(PlaceBetRequest request, String idempotencyKey);

    void consumeOddsFeed(List<OddsUpdate> feedEvents);

    SettleEventResponse settleEvent(String eventId, String winningSelection);

    List<UserBetSummaryResponse> getUserSummary(String userId);

    List<UserBetSummaryResponse> getUserSummary(String userId, int page, int size);

    List<UserBetSummaryResponse> getEventBets(String eventId, int page, int size);

    BetDetailResponse getBetById(String betId);

    CancelBetResponse cancelBet(String betId);

    UserExposureResponse getUserExposure(String userId);

    BigDecimal getTotalExposure();
}
