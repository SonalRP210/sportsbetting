package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.model.ExposureProjection;
import com.sonal.sportsbetting.model.ExposureProjectionKey;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.repository.ExposureProjectionRepository;
import com.sonal.sportsbetting.support.MoneyFormatting;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Order(5)
public class ExposureProjectionStartupInitializer implements ApplicationRunner {

    private final ExposureProjectionRepository exposureProjectionRepository;
    private final BetRepository betRepository;
    private final MoneyFormatting moneyFormatting;

    public ExposureProjectionStartupInitializer(
            ExposureProjectionRepository exposureProjectionRepository,
            BetRepository betRepository,
            MoneyFormatting moneyFormatting) {
        this.exposureProjectionRepository = exposureProjectionRepository;
        this.betRepository = betRepository;
        this.moneyFormatting = moneyFormatting;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (exposureProjectionRepository.count() > 0) {
            return;
        }
        BigDecimal totalOpen = moneyFormatting.normalize(betRepository.sumExposureByStatus(BetStatus.OPEN));
        exposureProjectionRepository.save(new ExposureProjection(ExposureProjectionKey.global(), totalOpen, 0));
        for (Object[] row : betRepository.sumOpenRiskGroupedByUser(BetStatus.OPEN)) {
            String userId = (String) row[0];
            BigDecimal risk = moneyFormatting.normalize((BigDecimal) row[1]);
            int count = ((Number) row[2]).intValue();
            exposureProjectionRepository.save(
                    new ExposureProjection(ExposureProjectionKey.forUser(userId), risk, count));
        }
    }
}
