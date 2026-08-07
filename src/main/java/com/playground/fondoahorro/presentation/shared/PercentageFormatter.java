package com.playground.fondoahorro.presentation.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Converts between basis points (stored/calculated form, e.g. 300) and the percentage shown to the user (e.g. "3"). */
public final class PercentageFormatter {

    private PercentageFormatter() {
    }

    public static String format(int basisPoints) {
        return BigDecimal.valueOf(basisPoints).movePointLeft(2).stripTrailingZeros().toPlainString();
    }

    public static Optional<Integer> parseToBasisPoints(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            BigDecimal percent = new BigDecimal(text.trim().replace(',', '.'));
            return Optional.of(percent.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact());
        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty();
        }
    }
}
