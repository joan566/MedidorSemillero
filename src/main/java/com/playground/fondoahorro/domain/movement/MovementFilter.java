package com.playground.fondoahorro.domain.movement;

import java.time.LocalDate;

/** All fields nullable — a null field means "no filter on this column". */
public record MovementFilter(
        LocalDate fromDate,
        LocalDate toDate,
        MovementKind kind,
        Fund fund,
        PaymentMethod paymentMethod,
        Long personId,
        Long movementTypeId) {

    public static MovementFilter empty() {
        return new MovementFilter(null, null, null, null, null, null, null);
    }
}
