package com.sonal.sportsbetting.support;

import com.sonal.sportsbetting.properties.BettingProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MoneyFormatting {

    private final int scale;
    private final RoundingMode roundingMode;

    public MoneyFormatting(BettingProperties bettingProperties) {
        this.scale = bettingProperties.moneyScale();
        this.roundingMode = RoundingMode.valueOf(bettingProperties.moneyRoundingMode());
    }

    public BigDecimal normalize(BigDecimal value) {
        return value.setScale(scale, roundingMode);
    }

    public int scale() {
        return scale;
    }

    public RoundingMode roundingMode() {
        return roundingMode;
    }
}
