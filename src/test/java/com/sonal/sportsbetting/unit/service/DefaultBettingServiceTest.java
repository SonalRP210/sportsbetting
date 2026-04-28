package com.sonal.sportsbetting.unit.service;

import com.sonal.sportsbetting.dto.request.PlaceBetRequest;
import com.sonal.sportsbetting.dto.response.BetDetailResponse;
import com.sonal.sportsbetting.dto.response.CancelBetResponse;
import com.sonal.sportsbetting.dto.response.PlaceBetResponse;
import com.sonal.sportsbetting.dto.response.SettleEventResponse;
import com.sonal.sportsbetting.dto.response.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.response.UserExposureResponse;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.service.BetPlacementService;
import com.sonal.sportsbetting.service.BetQueryService;
import com.sonal.sportsbetting.service.DefaultBettingService;
import com.sonal.sportsbetting.service.ExposureService;
import com.sonal.sportsbetting.service.OddsFeedPublisher;
import com.sonal.sportsbetting.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBettingServiceTest {

    @Mock
    private BetPlacementService betPlacementService;
    @Mock
    private OddsFeedPublisher oddsFeedPublisher;
    @Mock
    private SettlementService settlementService;
    @Mock
    private BetQueryService betQueryService;
    @Mock
    private ExposureService exposureService;

    private DefaultBettingService facade;

    @BeforeEach
    void setUp() {
        facade = new DefaultBettingService(
                betPlacementService,
                oddsFeedPublisher,
                settlementService,
                betQueryService,
                exposureService);
    }

    @Test
    void placeBetDelegatesToPlacement() {
        PlaceBetRequest req = new PlaceBetRequest("u", "e", "h", BigDecimal.ONE);
        PlaceBetResponse resp = new PlaceBetResponse("BET-1", new BigDecimal("2"), "OPEN");
        when(betPlacementService.placeBet(req, "k")).thenReturn(resp);
        assertEquals(resp, facade.placeBet(req, "k"));
        verify(betPlacementService).placeBet(req, "k");
    }

    @Test
    void consumeOddsFeedDelegates() {
        List<OddsUpdate> feed = List.of(new OddsUpdate());
        facade.consumeOddsFeed(feed);
        verify(oddsFeedPublisher).publish(feed);
    }

    @Test
    void settleEventDelegates() {
        SettleEventResponse r = new SettleEventResponse("e", "home", 1, 0, BigDecimal.TEN, BigDecimal.ONE);
        when(settlementService.settleEvent("e", "home")).thenReturn(r);
        assertEquals(r, facade.settleEvent("e", "home"));
        verify(settlementService).settleEvent("e", "home");
    }

    @Test
    void queriesAndCancelDelegate() {
        when(betQueryService.getUserSummary("u")).thenReturn(List.of());
        when(betQueryService.getUserSummary("u", 0, 10)).thenReturn(List.of());
        when(betQueryService.getEventBets("e", 0, 10)).thenReturn(List.of());
        BetDetailResponse detail = new BetDetailResponse();
        when(betQueryService.getBetById("b")).thenReturn(detail);
        CancelBetResponse cancel = new CancelBetResponse("b", "CANCELLED", "ok");
        when(betQueryService.cancelBet("b")).thenReturn(cancel);
        when(exposureService.getUserExposure("u")).thenReturn(new UserExposureResponse("u", BigDecimal.ZERO, 0));
        when(exposureService.getTotalExposure()).thenReturn(BigDecimal.TEN);

        assertEquals(List.of(), facade.getUserSummary("u"));
        assertEquals(List.of(), facade.getUserSummary("u", 0, 10));
        assertEquals(List.of(), facade.getEventBets("e", 0, 10));
        assertEquals(detail, facade.getBetById("b"));
        assertEquals(cancel, facade.cancelBet("b"));
        assertEquals(BigDecimal.TEN, facade.getTotalExposure());
        assertEquals(new UserExposureResponse("u", BigDecimal.ZERO, 0), facade.getUserExposure("u"));
    }
}
