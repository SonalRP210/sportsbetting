package com.sonal.sportsbetting.dto;

import java.math.BigDecimal;

public class BetDetailResponse {
    private String betId;
    private String userId;
    private String eventId;
    private String selection;
    private BigDecimal stake;
    private BigDecimal odds;
    private String status;

    public BetDetailResponse() {
    }

    public BetDetailResponse(String betId, String userId, String eventId, String selection, BigDecimal stake, BigDecimal odds, String status) {
        this.betId = betId;
        this.userId = userId;
        this.eventId = eventId;
        this.selection = selection;
        this.stake = stake;
        this.odds = odds;
        this.status = status;
    }

    public String getBetId() {
        return betId;
    }

    public void setBetId(String betId) {
        this.betId = betId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public BigDecimal getStake() {
        return stake;
    }

    public void setStake(BigDecimal stake) {
        this.stake = stake;
    }

    public BigDecimal getOdds() {
        return odds;
    }

    public void setOdds(BigDecimal odds) {
        this.odds = odds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
