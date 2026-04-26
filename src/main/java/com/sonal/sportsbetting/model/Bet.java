package com.sonal.sportsbetting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(
        name = "bets",
        uniqueConstraints = @UniqueConstraint(name = "uk_bets_user_idempotency", columnNames = {"user_id", "idempotency_key"})
)
public class Bet {
    @Id
    @Column(name = "bet_id", nullable = false, updatable = false, length = 64)
    private String betId;
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;
    @Column(name = "stake", nullable = false, precision = 19, scale = 2)
    private BigDecimal stake;
    @Column(name = "odds", nullable = false, precision = 19, scale = 2)
    private BigDecimal odds;
    @Column(name = "selection", nullable = false, length = 64)
    private String selection;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BetStatus status;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Version
    @Column(name = "opt_lock", nullable = false)
    private Long version;

    public Bet() {
    }

    public Bet(String betId, String userId, String eventId, BigDecimal stake, BigDecimal odds, String selection, BetStatus status) {
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

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public BetStatus getStatus() {
        return status;
    }

    public void setStatus(BetStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
