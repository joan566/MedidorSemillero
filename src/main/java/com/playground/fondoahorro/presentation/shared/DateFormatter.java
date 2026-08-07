package com.playground.fondoahorro.presentation.shared;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats dates as dd/MM/yyyy, e.g. "15/08/2026" — the only date format shown to the user. */
public final class DateFormatter {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DAY_MONTH_FORMAT =
            DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.of("es", "CO"));

    private DateFormatter() {
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(FORMAT);
    }

    /** e.g. "15 de agosto" — for birthday cards, where the year isn't the point. */
    public static String formatDayMonth(LocalDate date) {
        return date == null ? "" : date.format(DAY_MONTH_FORMAT);
    }
}
