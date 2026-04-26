package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.BetDetailResponse;
import com.sonal.sportsbetting.dto.CancelBetResponse;
import com.sonal.sportsbetting.dto.UserBetSummaryResponse;

import java.util.List;

public interface BetQueryService {
    List<UserBetSummaryResponse> getUserSummary(String userId);

    List<UserBetSummaryResponse> getUserSummary(String userId, int page, int size);

    List<UserBetSummaryResponse> getEventBets(String eventId, int page, int size);

    BetDetailResponse getBetById(String betId);

    CancelBetResponse cancelBet(String betId);
}
