package com.sonal.sportsbetting.controller;

import com.sonal.sportsbetting.config.AppConstants;
import com.sonal.sportsbetting.model.OddsUpdate;
import com.sonal.sportsbetting.service.BettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.base-path:/api/v1}")
public class OddsController {

    private final BettingService service;

    public OddsController(BettingService service) {
        this.service = service;
    }

    @PostMapping(AppConstants.Api.ODDS_FEED)
    public ResponseEntity<String> oddsFeed(@Valid @RequestBody List<@Valid OddsUpdate> updates) {
        service.consumeOddsFeed(updates);
        return ResponseEntity.accepted().body(AppConstants.Messages.ODDS_FEED_ACCEPTED);
    }
}
