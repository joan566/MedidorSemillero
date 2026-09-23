package com.playground.fondoahorro.presentation.shared;

import com.playground.fondoahorro.domain.vo.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

/** Formats Money as Colombian pesos, e.g. "$250.000" or "-$250.000" — never a raw number. */
public final class MoneyFormatter {

    private static final Locale LOCALE_CO = Locale.of("es", "CO");

    private MoneyFormatter() {
    }

    public static String format(Money money) {
        NumberFormat format = NumberFormat.getIntegerInstance(LOCALE_CO);
        BigDecimal pesos = money.toPesos().setScale(0, RoundingMode.HALF_UP);
        String sign = pesos.signum() < 0 ? "-" : "";
        return sign + "$" + format.format(pesos.abs());
    }

    /**
     * Parses a monetary amount typed by the user, ignoring any grouping
     * characters (e.g. "250.000" and "250000" both parse to 250000 pesos).
     * Empty if the field has no digits at all.
     */
    public static Optional<BigDecimal> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String digitsOnly = text.replaceAll("[^0-9]", "");
        if (digitsOnly.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(digitsOnly));
    }
}
