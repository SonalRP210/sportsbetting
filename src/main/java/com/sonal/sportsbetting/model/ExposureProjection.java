package com.sonal.sportsbetting.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "exposure_projection")
public class ExposureProjection {

    @EmbeddedId
    private ExposureProjectionKey id;

    @Column(name = "open_risk", nullable = false, precision = 19, scale = 2)
    private BigDecimal openRisk = BigDecimal.ZERO;

    @Column(name = "open_bet_count", nullable = false)
    private int openBetCount;

    public ExposureProjection() {
    }

    public ExposureProjection(ExposureProjectionKey id, BigDecimal openRisk, int openBetCount) {
        this.id = id;
        this.openRisk = openRisk;
        this.openBetCount = openBetCount;
    }

    public ExposureProjectionKey getId() {
        return id;
    }

    public void setId(ExposureProjectionKey id) {
        this.id = id;
    }

    public BigDecimal getOpenRisk() {
        return openRisk;
    }

    public void setOpenRisk(BigDecimal openRisk) {
        this.openRisk = openRisk;
    }

    public int getOpenBetCount() {
        return openBetCount;
    }

    public void setOpenBetCount(int openBetCount) {
        this.openBetCount = openBetCount;
    }
}
