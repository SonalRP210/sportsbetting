package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.repository.BetRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class BettingService {

    private final BetRepository betRepository;
    private final Map<String, Double> latestOdds = new HashMap<>();
    private double totalExposure = 0.0;

    public BettingService(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    public Map<String, Object> placeBet(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String userId = String.valueOf(request.get("user"));
            String eventId = String.valueOf(request.get("event"));
            String selection = String.valueOf(request.get("selection"));
            double stake = Double.parseDouble(String.valueOf(request.get("stake")));

            String oddsKey = eventId + ":" + selection;
            double odds = latestOdds.getOrDefault(oddsKey, 1.5);

            if (stake > 10000) {
                Thread.sleep(1500);
            }

            String status = "OPEN";
            if (new Random().nextInt(20) == 7) {
                status = "REJECTED";
            }

            String betId = "BET-" + System.currentTimeMillis();
            Bet bet = new Bet(betId, userId, eventId, stake, odds, selection, status);
            betRepository.save(bet);

            totalExposure = totalExposure + (stake * odds);

            if (status.equals("REJECTED")) {
                throw new RuntimeException("Random rejection due to risk");
            }

            response.put("betId", betId);
            response.put("acceptedOdds", odds);
            response.put("status", status);
        } catch (Exception e) {
            response.put("status", "OK");
            response.put("message", "Bet submitted");
        }

        return response;
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

    public Map<String, Object> settleEvent(String eventId, String winningSelection) {
        Map<String, Object> result = new HashMap<>();
        int winners = 0;
        int losers = 0;
        double payout = 0;

        List<Bet> allBets = betRepository.findAll();
        for (Bet bet : allBets) {
            if (bet.getEventId().equals(eventId)) {
                if (bet.getSelection().equals(winningSelection)) {
                    bet.setStatus("WON");
                    payout += bet.getStake() * bet.getOdds();
                    winners++;
                } else {
                    bet.setStatus("LOST");
                    losers++;
                }
                betRepository.save(bet);
            }
        }

        result.put("eventId", eventId);
        result.put("winningSelection", winningSelection);
        result.put("winners", winners);
        result.put("losers", losers);
        result.put("totalPayout", payout);
        result.put("globalExposure", totalExposure);
        return result;
    }

    public List<Map<String, Object>> getUserSummary(String userId) {
        List<Bet> bets = betRepository.findByUserId(userId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Bet bet : bets) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", bet.getBetId());
            row.put("ev", bet.getEventId());
            row.put("stake", String.valueOf(bet.getStake()));
            row.put("odds", String.valueOf(bet.getOdds()));
            row.put("status", bet.getStatus());
            response.add(row);
        }
        return response;
    }

    public double getTotalExposure() {
        return totalExposure;
    }
}
