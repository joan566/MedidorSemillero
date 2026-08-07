package com.playground.fondoahorro.presentation.shared;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Reformats a money TextField with thousand separators as the user types
 * (e.g. typing "250000" shows "250.000"). The caret jumps to the end after
 * each edit — a standard simplification for short money inputs like these.
 * MoneyFormatter.parse(field.getText()) keeps working unchanged, since it
 * already strips grouping separators.
 */
public final class MoneyTextFields {

    private static final Locale LOCALE_CO = Locale.of("es", "CO");

    private MoneyTextFields() {
    }

    public static void attachLiveFormatting(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            String digitsOnly = change.getControlNewText().replaceAll("[^0-9]", "");
            String formatted = digitsOnly.isEmpty() ? "" : format(digitsOnly);
            change.setText(formatted);
            change.setRange(0, change.getControlText().length());
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());
            return change;
        }));
    }

    private static String format(String digitsOnly) {
        return NumberFormat.getIntegerInstance(LOCALE_CO).format(new BigInteger(digitsOnly));
    }
}
