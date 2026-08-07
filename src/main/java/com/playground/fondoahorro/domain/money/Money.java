package com.playground.fondoahorro.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Amount of money in Colombian pesos. Wraps BigDecimal so no module ever
 * handles raw double/float or raw cent longs directly (see project rule:
 * money must never be a double/float).
 */
public final class Money implements Comparable<Money> {

    private static final int SCALE = 2;
    private static final BigDecimal CENTS_PER_UNIT = BigDecimal.valueOf(100);

    public static final Money ZERO = new Money(BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY));

    private final BigDecimal pesos;

    private Money(BigDecimal pesos) {
        this.pesos = pesos;
    }

    public static Money of(BigDecimal pesos) {
        if (pesos == null) {
            throw new IllegalArgumentException("El valor no puede ser nulo.");
        }
        return new Money(pesos.setScale(SCALE, RoundingMode.HALF_UP));
    }

    public static Money ofCents(long cents) {
        return new Money(BigDecimal.valueOf(cents).divide(CENTS_PER_UNIT, SCALE, RoundingMode.HALF_UP));
    }

    public long toCents() {
        return pesos.multiply(CENTS_PER_UNIT).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public BigDecimal toPesos() {
        return pesos;
    }

    public Money plus(Money other) {
        return new Money(pesos.add(other.pesos));
    }

    public Money minus(Money other) {
        return new Money(pesos.subtract(other.pesos));
    }

    public Money multiply(BigDecimal factor) {
        return new Money(pesos.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP));
    }

    public boolean isPositive() {
        return pesos.signum() > 0;
    }

    public boolean isZero() {
        return pesos.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        return pesos.compareTo(other.pesos) > 0;
    }

    @Override
    public int compareTo(Money other) {
        return pesos.compareTo(other.pesos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money other)) return false;
        return pesos.compareTo(other.pesos) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pesos.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return pesos.toPlainString();
    }
}
