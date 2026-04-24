package com.sonal.sportsbetting.model;

public class Bet {
    private String betId;
    private String userId;
    private String eventId;
    private double stake;
    private double odds;
    private String selection;
    private String status;

    public Bet() {
    }

    public Bet(String betId, String userId, String eventId, double stake, double odds, String selection, String status) {
        this.betId = betId;
        this.userId = userId;
        this.eventId = eventId;
        this.stake = stake;
        this.odds = odds;
        this.selection = selection;
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

    public double getStake() {
        return stake;
    }

    public void setStake(double stake) {
        this.stake = stake;
    }

    public double getOdds() {
        return odds;
    }

    public void setOdds(double odds) {
        this.odds = odds;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
