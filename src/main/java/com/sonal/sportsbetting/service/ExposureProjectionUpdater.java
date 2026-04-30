package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.domain.event.BetCancelledPayload;
import com.sonal.sportsbetting.domain.event.BetPlacedPayload;
import com.sonal.sportsbetting.domain.event.EventSettledPayload;
import com.sonal.sportsbetting.model.ExposureProjection;
import com.sonal.sportsbetting.model.ExposureProjectionKey;
import com.sonal.sportsbetting.repository.ExposureProjectionRepository;
import com.sonal.sportsbetting.support.MoneyFormatting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ExposureProjectionUpdater {

    private final ExposureProjectionRepository exposureProjectionRepository;
    private final MoneyFormatting moneyFormatting;

    public ExposureProjectionUpdater(
            ExposureProjectionRepository exposureProjectionRepository,
            MoneyFormatting moneyFormatting) {
        this.exposureProjectionRepository = exposureProjectionRepository;
        this.moneyFormatting = moneyFormatting;
    }

    @Transactional
    public void applyBetPlaced(BetPlacedPayload payload) {
        BigDecimal delta = moneyFormatting.normalize(payload.openRisk());
        addGlobal(delta);
        addUser(payload.userId(), delta);
    }

    @Transactional
    public void applyBetCancelled(BetCancelledPayload payload) {
        BigDecimal delta = moneyFormatting.normalize(payload.openRisk());
        subtractGlobal(delta);
        subtractUser(payload.userId(), delta);
    }

    @Transactional
    public void applyEventSettled(EventSettledPayload payload) {
        for (EventSettledPayload.RiskRelease release : payload.releases()) {
            BigDecimal amount = moneyFormatting.normalize(release.openRisk());
            subtractGlobal(amount);
            subtractUser(release.userId(), amount);
        }
    }

    private void addGlobal(BigDecimal delta) {
        ExposureProjection row = globalRow();
        row.setOpenRisk(moneyFormatting.normalize(row.getOpenRisk().add(delta)));
        exposureProjectionRepository.save(row);
    }

    private void subtractGlobal(BigDecimal delta) {
        ExposureProjection row = globalRow();
        row.setOpenRisk(moneyFormatting.normalize(row.getOpenRisk().subtract(delta)).max(BigDecimal.ZERO));
        exposureProjectionRepository.save(row);
    }

    private void addUser(String userId, BigDecimal delta) {
        ExposureProjection row = userRow(userId);
        row.setOpenRisk(moneyFormatting.normalize(row.getOpenRisk().add(delta)));
        row.setOpenBetCount(row.getOpenBetCount() + 1);
        exposureProjectionRepository.save(row);
    }

    private void subtractUser(String userId, BigDecimal delta) {
        ExposureProjection row = userRow(userId);
        row.setOpenRisk(moneyFormatting.normalize(row.getOpenRisk().subtract(delta)).max(BigDecimal.ZERO));
        row.setOpenBetCount(Math.max(0, row.getOpenBetCount() - 1));
        exposureProjectionRepository.save(row);
    }

    private ExposureProjection globalRow() {
        return exposureProjectionRepository
                .findByIdScopeAndIdUserId(ExposureProjectionKey.SCOPE_GLOBAL, "")
                .orElseGet(() -> exposureProjectionRepository.save(
                        new ExposureProjection(ExposureProjectionKey.global(), BigDecimal.ZERO, 0)));
    }

    private ExposureProjection userRow(String userId) {
        return exposureProjectionRepository
                .findByIdScopeAndIdUserId(ExposureProjectionKey.SCOPE_USER, userId)
                .orElseGet(() -> exposureProjectionRepository.save(
                        new ExposureProjection(ExposureProjectionKey.forUser(userId), BigDecimal.ZERO, 0)));
    }
}
