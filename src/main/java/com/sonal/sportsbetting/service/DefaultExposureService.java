package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.response.UserExposureResponse;
import com.sonal.sportsbetting.model.ExposureProjection;
import com.sonal.sportsbetting.model.ExposureProjectionKey;
import com.sonal.sportsbetting.repository.ExposureProjectionRepository;
import com.sonal.sportsbetting.support.MoneyFormatting;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DefaultExposureService implements ExposureService {

    private final ExposureProjectionRepository exposureProjectionRepository;
    private final MoneyFormatting moneyFormatting;
    private final AtomicReference<BigDecimal> totalExposure = new AtomicReference<>(BigDecimal.ZERO);

    public DefaultExposureService(
            ExposureProjectionRepository exposureProjectionRepository,
            MoneyFormatting moneyFormatting,
            MeterRegistry meterRegistry) {
        this.exposureProjectionRepository = exposureProjectionRepository;
        this.moneyFormatting = moneyFormatting;
        meterRegistry.gauge("bets.exposure.total", totalExposure, a -> a.get().doubleValue());
    }

    @Override
    public BigDecimal getTotalExposure() {
        BigDecimal value = exposureProjectionRepository
                .findById(ExposureProjectionKey.global())
                .map(ExposureProjection::getOpenRisk)
                .map(moneyFormatting::normalize)
                .orElse(BigDecimal.ZERO);
        totalExposure.set(value);
        return value;
    }

    @Override
    public UserExposureResponse getUserExposure(String userId) {
        return exposureProjectionRepository
                .findById(ExposureProjectionKey.forUser(userId))
                .map(row -> new UserExposureResponse(
                        userId,
                        moneyFormatting.normalize(row.getOpenRisk()),
                        row.getOpenBetCount()))
                .orElseGet(() -> new UserExposureResponse(userId, BigDecimal.ZERO, 0));
    }
}
