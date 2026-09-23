package com.playground.fondoahorro.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void ofCentsAndToCentsRoundTripExactly() {
        Money money = Money.ofCents(250_000_00L);
        assertEquals(250_000_00L, money.toCents());
        assertEquals(0, new BigDecimal("250000.00").compareTo(money.toPesos()));
    }

    @Test
    void ofPesosAndToCentsAgree() {
        Money money = Money.of(new BigDecimal("1500"));
        assertEquals(150_000L, money.toCents());
    }

    @Test
    void plusAddsCorrectly() {
        Money a = Money.of(new BigDecimal("100000"));
        Money b = Money.of(new BigDecimal("50000"));
        assertEquals(Money.of(new BigDecimal("150000")), a.plus(b));
    }

    @Test
    void minusSubtractsCorrectly() {
        Money a = Money.of(new BigDecimal("100000"));
        Money b = Money.of(new BigDecimal("30000"));
        assertEquals(Money.of(new BigDecimal("70000")), a.minus(b));
    }

    @Test
    void minusCanGoNegative() {
        Money a = Money.of(new BigDecimal("100"));
        Money b = Money.of(new BigDecimal("300"));
        Money result = a.minus(b);
        assertEquals(Money.of(new BigDecimal("-200")), result);
        assertFalse(result.isPositive());
    }

    @Test
    void multiplyAppliesRateWithRounding() {
        // this is exactly how Loan/Settlement turn interestRateBps into an amount
        Money principal = Money.of(new BigDecimal("1000000"));
        Money interest = principal.multiply(new BigDecimal("0.03"));
        assertEquals(Money.of(new BigDecimal("30000")), interest);
    }

    @Test
    void isZeroAndIsPositive() {
        assertTrue(Money.ZERO.isZero());
        assertFalse(Money.ZERO.isPositive());
        assertTrue(Money.of(new BigDecimal("1")).isPositive());
    }

    @Test
    void isGreaterThanComparesValue() {
        Money bigger = Money.of(new BigDecimal("200"));
        Money smaller = Money.of(new BigDecimal("100"));
        assertTrue(bigger.isGreaterThan(smaller));
        assertFalse(smaller.isGreaterThan(bigger));
        assertFalse(bigger.isGreaterThan(bigger));
    }

    @Test
    void equalsIgnoresTrailingScaleDifferences() {
        Money a = Money.of(new BigDecimal("100"));
        Money b = Money.of(new BigDecimal("100.00"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ofRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(null));
    }
}
