package com.sonal.sportsbetting.controller;

import com.sonal.sportsbetting.config.AppConstants;
import com.sonal.sportsbetting.dto.request.PlaceBetRequest;
import com.sonal.sportsbetting.dto.response.BetDetailResponse;
import com.sonal.sportsbetting.dto.response.CancelBetResponse;
import com.sonal.sportsbetting.dto.response.PlaceBetResponse;
import com.sonal.sportsbetting.service.BettingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.base-path:/api/v1}")
public class BetController {

    private final BettingService service;

    public BetController(BettingService service) {
        this.service = service;
    }

    @PostMapping(AppConstants.Api.BETS)
    public ResponseEntity<PlaceBetResponse> placeBet(
            @Valid @RequestBody PlaceBetRequest req,
            @RequestHeader(value = "${app.http.idempotency-key-header}", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.placeBet(req, idempotencyKey));
    }

    @GetMapping(AppConstants.Api.BET_BY_ID)
    public ResponseEntity<BetDetailResponse> getBet(@PathVariable String betId) {
        return ResponseEntity.ok(service.getBetById(betId));
    }

    @PostMapping(AppConstants.Api.BET_CANCEL)
    public ResponseEntity<CancelBetResponse> cancelBet(@PathVariable String betId) {
        return ResponseEntity.ok(service.cancelBet(betId));
    }
}
