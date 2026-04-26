package com.sonal.sportsbetting.repository;

import com.sonal.sportsbetting.model.Bet;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class BetRepository {

    private static final Map<String, Bet> betTable = new HashMap<>();

    public Bet save(Bet bet) {
        betTable.put(bet.getBetId(), bet);
        return bet;
    }

    public Bet findById(String id) {
        return betTable.get(id);
    }

    public List<Bet> findByUserId(String userId) {
        List<Bet> bets = new ArrayList<>();
        for (String id : betTable.keySet()) {
            Bet bet = betTable.get(id);
            if (bet != null && bet.getUserId() != null && bet.getUserId().equals(userId)) {
                bets.add(bet);
            }
        }
        return bets;
    }

    public List<Bet> findAll() {
        return new ArrayList<>(betTable.values());
    }

    public List<Bet> findByEventId(String eventId) {
        List<Bet> bets = new ArrayList<>();
        for (Bet bet : betTable.values()) {
            if (bet != null && eventId.equals(bet.getEventId())) {
                bets.add(bet);
            }
        }
        return bets;
    }
}
