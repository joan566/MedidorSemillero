package com.playground.fondoahorro.domain.savings;

import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Money a person has contributed to the fund. Belongs conceptually to that person. */
public record Saving(
        Long id,
        long personId,
        Money amount,
        LocalDate date,
        PaymentMethod paymentMethod,
        String notes,
        LocalDateTime createdAt) {

    public Saving {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Ingresa un valor mayor a $0.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Selecciona la fecha del ahorro.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del ahorro no puede ser futura.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Selecciona el medio de pago.");
        }
        notes = (notes == null || notes.isBlank()) ? null : notes.trim();
    }

    public static Saving create(long personId, Money amount, LocalDate date, PaymentMethod paymentMethod, String notes) {
        return new Saving(null, personId, amount, date, paymentMethod, notes, null);
    }

    public Saving withUpdatedDetails(Money amount, LocalDate date, PaymentMethod paymentMethod, String notes) {
        return new Saving(id, personId, amount, date, paymentMethod, notes, createdAt);
    }
}
