package com.sonal.sportsbetting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "event_settlements")
public class EventSettlement {

    @Id
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "winning_selection", nullable = false, length = 64)
    private String winningSelection;

    @Column(name = "winners", nullable = false)
    private int winners;

    @Column(name = "losers", nullable = false)
    private int losers;

    @Column(name = "total_payout", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPayout;

    public EventSettlement() {
    }

    public EventSettlement(
            String eventId,
            String winningSelection,
            int winners,
            int losers,
            BigDecimal totalPayout) {
        this.eventId = eventId;
        this.winningSelection = winningSelection;
        this.winners = winners;
        this.losers = losers;
        this.totalPayout = totalPayout;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getWinningSelection() {
        return winningSelection;
    }

    public void setWinningSelection(String winningSelection) {
        this.winningSelection = winningSelection;
    }

    public int getWinners() {
        return winners;
    }

    public void setWinners(int winners) {
        this.winners = winners;
    }

    public int getLosers() {
        return losers;
    }

    public void setLosers(int losers) {
        this.losers = losers;
    }

    public BigDecimal getTotalPayout() {
        return totalPayout;
    }

    public void setTotalPayout(BigDecimal totalPayout) {
        this.totalPayout = totalPayout;
    }
}
