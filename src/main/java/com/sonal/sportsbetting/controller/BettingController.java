package com.sonal.sportsbetting.controller;

import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.service.BettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BettingController {

    private final BettingService service;

    public BettingController(BettingService service) {
        this.service = service;
    }

    @PostMapping("/bet")
    public ResponseEntity<Map<String, Object>> bet(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(service.placeBet(req));
    }

    @PostMapping("/odds-feed")
    public ResponseEntity<String> oddsFeed(@RequestBody List<OddsUpdate> updates) {
        service.consumeOddsFeed(updates);
        return ResponseEntity.ok("done");
    }

    @PostMapping("/settle")
    public ResponseEntity<Map<String, Object>> settle(
            @RequestParam String eventId,
            @RequestParam String winner) {
        return ResponseEntity.ok(service.settleEvent(eventId, winner));
    }

    @GetMapping("/user")
    public ResponseEntity<List<Map<String, Object>>> user(@RequestParam String id) {
        return ResponseEntity.ok(service.getUserSummary(id));
    }

    @GetMapping("/health")
    public String health() {
        return "up";
    }
}
