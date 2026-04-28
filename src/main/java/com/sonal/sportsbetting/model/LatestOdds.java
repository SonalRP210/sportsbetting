package com.sonal.sportsbetting.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "latest_odds")
public class LatestOdds {

    @EmbeddedId
    private LatestOddsId id;

    @Column(name = "odds", nullable = false, precision = 19, scale = 2)
    private BigDecimal odds;

    public LatestOdds() {
    }

    public LatestOdds(LatestOddsId id, BigDecimal odds) {
        this.id = id;
        this.odds = odds;
    }

    public LatestOddsId getId() {
        return id;
    }

    public void setId(LatestOddsId id) {
        this.id = id;
    }

    public BigDecimal getOdds() {
        return odds;
    }

    public void setOdds(BigDecimal odds) {
        this.odds = odds;
    }
}
