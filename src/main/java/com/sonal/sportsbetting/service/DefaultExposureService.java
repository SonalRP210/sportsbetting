package com.sonal.sportsbetting.service;

import com.sonal.sportsbetting.dto.response.UserExposureResponse;
import com.sonal.sportsbetting.model.Bet;
import com.sonal.sportsbetting.model.BetStatus;
import com.sonal.sportsbetting.repository.BetRepository;
import com.sonal.sportsbetting.support.MoneyFormatting;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DefaultExposureService implements ExposureService {

    private final BetRepository betRepository;
    private final MoneyFormatting moneyFormatting;
    private final AtomicReference<BigDecimal> totalExposure = new AtomicReference<>(BigDecimal.ZERO);

    public DefaultExposureService(
            BetRepository betRepository,
            MoneyFormatting moneyFormatting,
            MeterRegistry meterRegistry) {
        this.betRepository = betRepository;
        this.moneyFormatting = moneyFormatting;
        meterRegistry.gauge("bets.exposure.total", totalExposure, a -> a.get().doubleValue());
    }

    @Override
    public void increaseExposure(BigDecimal amount) {
        totalExposure.updateAndGet(current -> moneyFormatting.normalize(current.add(amount)));
    }

    @Override
    public void decreaseExposure(BigDecimal amount) {
        totalExposure.updateAndGet(current -> moneyFormatting.normalize(current.subtract(amount)).max(BigDecimal.ZERO));
    }

    @Override
    public BigDecimal getTotalExposure() {
        return totalExposure.get();
    }

    @Override
    public UserExposureResponse getUserExposure(String userId) {
        List<Bet> bets = betRepository.findByUserId(userId);
        BigDecimal exposure = BigDecimal.ZERO;
        int openBets = 0;
        for (Bet bet : bets) {
            if (bet.getStatus() == BetStatus.OPEN) {
                openBets++;
                exposure = moneyFormatting.normalize(
                        exposure.add(moneyFormatting.normalize(bet.getStake().multiply(bet.getOdds()))));
            }
        }
        return new UserExposureResponse(userId, exposure, openBets);
    }
}
