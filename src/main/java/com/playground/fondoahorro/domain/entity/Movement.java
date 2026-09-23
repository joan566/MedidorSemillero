package com.playground.fondoahorro.domain.entity;

import com.playground.fondoahorro.domain.vo.Money;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.enums.MovementKind;

/**
 * One entry in the central financial ledger. Every module that moves money
 * (savings, loans, loan payments, birthday gifts, settlements) writes one of
 * these, in the same transaction as its own entity — fund balances are
 * always derived by summing this table, never stored separately.
 */
public record Movement(
        Long id,
        long movementTypeId,
        Fund fund,
        PaymentMethod paymentMethod,
        MovementKind kind,
        Money amount,
        LocalDate date,
        Long personId,
        String referenceTable,
        Long referenceId,
        String notes,
        LocalDateTime createdAt) {

    public Movement {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("El valor del movimiento debe ser mayor a $0.");
        }
        if (fund == null) {
            throw new IllegalArgumentException("Selecciona el fondo del movimiento.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Selecciona el medio de pago.");
        }
        if (kind == null) {
            throw new IllegalArgumentException("El movimiento debe ser de ingreso o egreso.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Selecciona la fecha del movimiento.");
        }
    }

    public static Movement create(long movementTypeId, Fund fund, PaymentMethod paymentMethod, MovementKind kind,
                                   Money amount, LocalDate date, Long personId, String referenceTable,
                                   Long referenceId, String notes) {
        return new Movement(null, movementTypeId, fund, paymentMethod, kind, amount, date, personId,
                referenceTable, referenceId, notes, null);
    }

    public Movement withUpdatedDetails(PaymentMethod paymentMethod, Money amount, LocalDate date, String notes) {
        return new Movement(id, movementTypeId, fund, paymentMethod, kind, amount, date, personId,
                referenceTable, referenceId, notes, createdAt);
    }
}
