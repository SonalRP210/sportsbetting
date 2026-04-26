package com.sonal.sportsbetting.unit.support;

import com.sonal.sportsbetting.PropertyFixtures;
import com.sonal.sportsbetting.support.MoneyFormatting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyFormattingTest {

    @Test
    void normalizeUsesConfiguredScaleAndHalfUp() {
        MoneyFormatting mf = PropertyFixtures.moneyFormatting();
        assertEquals(new BigDecimal("1.76"), mf.normalize(new BigDecimal("1.755")));
        assertEquals(new BigDecimal("10.00"), mf.normalize(new BigDecimal("9.999")));
    }

    @Test
    void exposesScaleAndRoundingFromProperties() {
        MoneyFormatting mf = PropertyFixtures.moneyFormatting();
        assertEquals(2, mf.scale());
        assertEquals(RoundingMode.HALF_UP, mf.roundingMode());
    }
}
