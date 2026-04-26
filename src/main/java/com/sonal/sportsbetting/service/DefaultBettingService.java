package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.BetDetailResponse;
import com.sonal.sportsbetting.dto.CancelBetResponse;
import com.sonal.sportsbetting.dto.PlaceBetRequest;
import com.sonal.sportsbetting.dto.PlaceBetResponse;
import com.sonal.sportsbetting.dto.SettleEventResponse;
import com.sonal.sportsbetting.dto.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.UserExposureResponse;
import com.sonal.sportsbetting.model.OddsUpdate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DefaultBettingService implements BettingService {
    private final BetPlacementService betPlacementService;
    private final OddsService oddsService;
    private final SettlementService settlementService;
    private final BetQueryService betQueryService;
    private final ExposureService exposureService;

    public DefaultBettingService(
            BetPlacementService betPlacementService,
            OddsService oddsService,
            SettlementService settlementService,
            BetQueryService betQueryService,
            ExposureService exposureService) {
        this.betPlacementService = betPlacementService;
        this.oddsService = oddsService;
        this.settlementService = settlementService;
        this.betQueryService = betQueryService;
        this.exposureService = exposureService;
    }

    @Override
    public PlaceBetResponse placeBet(PlaceBetRequest request, String idempotencyKey) {
        return betPlacementService.placeBet(request, idempotencyKey);
    }

    @Override
    public void consumeOddsFeed(List<OddsUpdate> feedEvents) {
        oddsService.consumeOddsFeed(feedEvents);
    }

    @Override
    public SettleEventResponse settleEvent(String eventId, String winningSelection) {
        return settlementService.settleEvent(eventId, winningSelection);
    }

    @Override
    public List<UserBetSummaryResponse> getUserSummary(String userId) {
        return betQueryService.getUserSummary(userId);
    }

    @Override
    public List<UserBetSummaryResponse> getUserSummary(String userId, int page, int size) {
        return betQueryService.getUserSummary(userId, page, size);
    }

    @Override
    public List<UserBetSummaryResponse> getEventBets(String eventId, int page, int size) {
        return betQueryService.getEventBets(eventId, page, size);
    }

    @Override
    public BetDetailResponse getBetById(String betId) {
        return betQueryService.getBetById(betId);
    }

    @Override
    public CancelBetResponse cancelBet(String betId) {
        return betQueryService.cancelBet(betId);
    }

    @Override
    public UserExposureResponse getUserExposure(String userId) {
        return exposureService.getUserExposure(userId);
    }

    @Override
    public BigDecimal getTotalExposure() {
        return exposureService.getTotalExposure();
    }
}
