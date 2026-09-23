package com.playground.fondoahorro.domain.vo;

import java.time.LocalDate;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.enums.PaymentMethod;

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
