package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.request.PlaceBetRequest;
import com.sonal.sportsbetting.dto.response.BetDetailResponse;
import com.sonal.sportsbetting.dto.response.CancelBetResponse;
import com.sonal.sportsbetting.dto.response.PlaceBetResponse;
import com.sonal.sportsbetting.dto.response.SettleEventResponse;
import com.sonal.sportsbetting.dto.response.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.response.UserExposureResponse;
import com.sonal.sportsbetting.model.OddsUpdate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DefaultBettingService implements BettingService {
    private final BetPlacementService betPlacementService;
    private final OddsFeedPublisher oddsFeedPublisher;
    private final SettlementService settlementService;
    private final BetQueryService betQueryService;
    private final ExposureService exposureService;

    public DefaultBettingService(
            BetPlacementService betPlacementService,
            OddsFeedPublisher oddsFeedPublisher,
            SettlementService settlementService,
            BetQueryService betQueryService,
            ExposureService exposureService) {
        this.betPlacementService = betPlacementService;
        this.oddsFeedPublisher = oddsFeedPublisher;
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
        oddsFeedPublisher.publish(feedEvents);
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
