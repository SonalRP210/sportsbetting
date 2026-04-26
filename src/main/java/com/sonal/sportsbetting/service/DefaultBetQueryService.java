package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.response.BetDetailResponse;
import com.sonal.sportsbetting.dto.response.CancelBetResponse;
import com.sonal.sportsbetting.dto.response.UserBetSummaryResponse;
import com.sonal.sportsbetting.exception.BetNotFoundException;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.repository.BetRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DefaultBetQueryService implements BetQueryService {
    private final BetRepository betRepository;
    private final MeterRegistry meterRegistry;

    public DefaultBetQueryService(BetRepository betRepository, MeterRegistry meterRegistry) {
        this.betRepository = betRepository;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<UserBetSummaryResponse> getUserSummary(String userId) {
        return mapUserBets(betRepository.findByUserId(userId));
    }

    @Override
    public List<UserBetSummaryResponse> getUserSummary(String userId, int page, int size) {
        List<Bet> bets = betRepository.findByUserId(userId);
        return mapUserBets(slice(bets, page, size));
    }

    @Override
    public List<UserBetSummaryResponse> getEventBets(String eventId, int page, int size) {
        List<Bet> eventBets = betRepository.findByEventId(eventId);
        return mapUserBets(slice(eventBets, page, size));
    }

    @Override
    public BetDetailResponse getBetById(String betId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new BetNotFoundException("Bet not found: " + betId));
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

    @Override
    @Transactional
    public CancelBetResponse cancelBet(String betId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new BetNotFoundException("Bet not found: " + betId));
        if (bet.getStatus() != BetStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN bets can be cancelled");
        }
        bet.setStatus(BetStatus.CANCELLED);
        betRepository.save(bet);
        meterRegistry.counter("bets.cancelled.total").increment();
        return new CancelBetResponse(bet.getBetId(), bet.getStatus().name(), "Bet cancelled");
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
