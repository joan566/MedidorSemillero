package com.playground.fondoahorro.domain.settlement;

import com.playground.fondoahorro.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementTest {

    private static Money pesos(String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    void calculateAppliesTheDefaultThreePercentRate() {
        Settlement settlement = Settlement.calculate(1L, 2026, pesos("2000000"), 300);

        assertEquals(pesos("60000"), settlement.interestAmount());
        assertEquals(pesos("2060000"), settlement.totalAmount());
    }

    @Test
    void calculateAppliesOtherRates() {
        Settlement settlement = Settlement.calculate(1L, 2026, pesos("1500000"), 500);

        assertEquals(pesos("75000"), settlement.interestAmount());
        assertEquals(pesos("1575000"), settlement.totalAmount());
    }

    @Test
    void personWithNoSavingsThatYearGetsZeroWithoutError() {
        Settlement settlement = Settlement.calculate(1L, 2026, Money.ZERO, 300);

        assertEquals(Money.ZERO, settlement.interestAmount());
        assertEquals(Money.ZERO, settlement.totalAmount());
    }

    @Test
    void calculateRejectsNegativeInterestRate() {
        assertThrows(IllegalArgumentException.class,
                () -> Settlement.calculate(1L, 2026, pesos("100000"), -1));
    }

    @Test
    void calculateRejectsNullSavingsTotal() {
        assertThrows(IllegalArgumentException.class,
                () -> Settlement.calculate(1L, 2026, null, 300));
    }
}
