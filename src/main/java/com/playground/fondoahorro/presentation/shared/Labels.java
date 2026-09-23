package com.playground.fondoahorro.presentation.shared;

import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.enums.PaymentMethod;

/** Spanish display labels for the domain enums shown across every screen. */
public final class Labels {

    private Labels() {
    }

    public static String of(Fund fund) {
        return switch (fund) {
            case SAVINGS -> "Fondo de ahorro";
            case BIRTHDAY -> "Fondo de cumpleaños";
        };
    }

    public static String of(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Efectivo";
            case TRANSFER -> "Transferencia";
        };
    }

    public static String of(MovementKind kind) {
        return switch (kind) {
            case INCOME -> "Ingreso";
            case EXPENSE -> "Egreso";
        };
    }
}
