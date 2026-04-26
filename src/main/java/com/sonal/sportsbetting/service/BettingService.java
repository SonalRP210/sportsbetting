package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.BetDetailResponse;
import com.sonal.sportsbetting.dto.CancelBetResponse;
import com.sonal.sportsbetting.dto.PlaceBetRequest;
import com.sonal.sportsbetting.dto.PlaceBetResponse;
import com.sonal.sportsbetting.dto.SettleEventResponse;
import com.sonal.sportsbetting.dto.UserBetSummaryResponse;
import com.sonal.sportsbetting.dto.UserExposureResponse;
import com.sonal.sportsbetting.exception.BetNotFoundException;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.BetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class BettingService {

    private static final BigDecimal DEFAULT_ODDS = new BigDecimal("1.50");
    private static final BigDecimal STAKE_SLEEP_THRESHOLD = new BigDecimal("10000");
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUND = RoundingMode.HALF_UP;

    private final BetRepository betRepository;
    private final Map<String, BigDecimal> latestOdds = new HashMap<>();
    private BigDecimal totalExposure = BigDecimal.ZERO;

    public BettingService(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUND);
    }

    public PlaceBetResponse placeBet(PlaceBetRequest request) {
        String oddsKey = request.eventId() + ":" + request.selection();
        BigDecimal odds = latestOdds.getOrDefault(oddsKey, DEFAULT_ODDS);

        if (request.stake().compareTo(STAKE_SLEEP_THRESHOLD) > 0) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread interrupted while processing bet", e);
            }
        }

        BetStatus status = BetStatus.OPEN;
        if (new Random().nextInt(20) == 7) {
            status = BetStatus.REJECTED;
        }

        String betId = "BET-" + System.currentTimeMillis();
        Bet bet = new Bet(betId, request.userId(), request.eventId(), request.stake(), odds, request.selection(), status);
        betRepository.save(bet);

        totalExposure = money(totalExposure.add(money(request.stake().multiply(odds))));

        if (status == BetStatus.REJECTED) {
            throw new IllegalArgumentException("Bet rejected due to risk rules");
        }

        return new PlaceBetResponse(betId, odds, status.name());
    }

    public void consumeOddsFeed(List<OddsUpdate> feedEvents) {
        for (OddsUpdate u : feedEvents) {
            String key = u.getEventId() + ":" + u.getSelection();
            latestOdds.put(key, u.getOdds());

            List<Bet> all = betRepository.findAll();
            for (Bet bet : all) {
                if (bet.getEventId().equals(u.getEventId()) && bet.getSelection().equalsIgnoreCase(u.getSelection())) {
                    bet.setOdds(u.getOdds());
                    betRepository.save(bet);
                }
            }
        }
    }

    public SettleEventResponse settleEvent(String eventId, String winningSelection) {
        int winners = 0;
        int losers = 0;
        BigDecimal payout = BigDecimal.ZERO;

        List<Bet> allBets = betRepository.findAll();
        for (Bet bet : allBets) {
            if (bet.getEventId().equals(eventId)) {
                if (bet.getSelection().equals(winningSelection)) {
                    bet.setStatus(BetStatus.WON);
                    payout = money(payout.add(money(bet.getStake().multiply(bet.getOdds()))));
                    winners++;
                } else {
                    bet.setStatus(BetStatus.LOST);
                    losers++;
                }
                betRepository.save(bet);
            }
        }

        return new SettleEventResponse(eventId, winningSelection, winners, losers, payout, totalExposure);
    }

    public List<UserBetSummaryResponse> getUserSummary(String userId) {
        return mapUserBets(betRepository.findByUserId(userId));
    }

    public List<UserBetSummaryResponse> getUserSummary(String userId, int page, int size) {
        List<Bet> bets = betRepository.findByUserId(userId);
        return mapUserBets(slice(bets, page, size));
    }

    public List<UserBetSummaryResponse> getEventBets(String eventId, int page, int size) {
        List<Bet> eventBets = betRepository.findByEventId(eventId);
        return mapUserBets(slice(eventBets, page, size));
    }

    public BetDetailResponse getBetById(String betId) {
        Bet bet = betRepository.findById(betId);
        if (bet == null) {
            throw new BetNotFoundException("Bet not found: " + betId);
        }
        return new BetDetailResponse(
                bet.getBetId(),
                bet.getUserId(),
                bet.getEventId(),
                bet.getSelection(),
                bet.getStake(),
                bet.getOdds(),
                bet.getStatus().name()
        );
    }

    public CancelBetResponse cancelBet(String betId) {
        Bet bet = betRepository.findById(betId);
        if (bet == null) {
            throw new BetNotFoundException("Bet not found: " + betId);
        }
        if (bet.getStatus() != BetStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN bets can be cancelled");
        }
        bet.setStatus(BetStatus.CANCELLED);
        betRepository.save(bet);
        return new CancelBetResponse(bet.getBetId(), bet.getStatus().name(), "Bet cancelled");
    }

    public UserExposureResponse getUserExposure(String userId) {
        List<Bet> bets = betRepository.findByUserId(userId);
        BigDecimal exposure = BigDecimal.ZERO;
        int openBets = 0;
        for (Bet bet : bets) {
            if (bet.getStatus() == BetStatus.OPEN) {
                openBets++;
                exposure = money(exposure.add(money(bet.getStake().multiply(bet.getOdds()))));
            }
        }
        return new UserExposureResponse(userId, exposure, openBets);
    }

    public BigDecimal getTotalExposure() {
        return totalExposure;
    }

    private List<UserBetSummaryResponse> mapUserBets(List<Bet> bets) {
        List<UserBetSummaryResponse> response = new ArrayList<>();
        for (Bet bet : bets) {
            response.add(new UserBetSummaryResponse(
                    bet.getBetId(),
                    bet.getEventId(),
                    bet.getStake(),
                    bet.getOdds(),
                    bet.getStatus().name()
            ));
        }
        return response;
    }

    private List<Bet> slice(List<Bet> input, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        List<Bet> ordered = new ArrayList<>(input);
        ordered.sort(Comparator.comparing(Bet::getBetId));
        int from = safePage * safeSize;
        if (from >= ordered.size()) {
            return List.of();
        }
        int to = Math.min(from + safeSize, ordered.size());
        return ordered.subList(from, to);
    }
}
