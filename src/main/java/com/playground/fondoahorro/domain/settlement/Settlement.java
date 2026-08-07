package com.playground.fondoahorro.domain.settlement;

import com.playground.fondoahorro.domain.money.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What a participant is owed for a given year: that year's savings plus
 * interest. Calculated from that year's savings only — a fresh calculation
 * each year, not a running balance carried across years (see project
 * decision). Never moves money on its own; see SettlementService.
 */
public record Settlement(
        Long id,
        long personId,
        int year,
        Money savingsTotal,
        int interestRateBps,
        Money interestAmount,
        Money totalAmount,
        LocalDateTime preparedAt) {

    public Settlement {
        if (savingsTotal == null) {
            throw new IllegalArgumentException("El total de ahorro no puede ser nulo.");
        }
        if (interestRateBps < 0) {
            throw new IllegalArgumentException("La tasa de interés no puede ser negativa.");
        }
        if (interestAmount == null || totalAmount == null) {
            throw new IllegalArgumentException("El cálculo de la liquidación es inválido.");
        }
    }

    public static Settlement calculate(long personId, int year, Money savingsTotal, int interestRateBps) {
        if (savingsTotal == null) {
            throw new IllegalArgumentException("El total de ahorro no puede ser nulo.");
        }
        Money interestAmount = savingsTotal.multiply(rateFromBps(interestRateBps));
        Money totalAmount = savingsTotal.plus(interestAmount);
        return new Settlement(null, personId, year, savingsTotal, interestRateBps, interestAmount, totalAmount, null);
    }

    private static BigDecimal rateFromBps(int bps) {
        return BigDecimal.valueOf(bps).movePointLeft(4);
    }
}
