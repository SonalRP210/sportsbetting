package com.sonal.sportsbetting;

import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.service.BettingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class BettingServiceTest {

    @Test
    void shouldPlaceBet() {
        BettingService service = new BettingService(new BetRepository());
        Map<String, Object> req = new HashMap<>();
        req.put("user", "u1");
        req.put("event", "e1");
        req.put("selection", "home");
        req.put("stake", "20");

        Map<String, Object> response = service.placeBet(req);
        Assertions.assertNotNull(response.get("status"));
    }
}
