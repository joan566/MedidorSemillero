package com.playground.fondoahorro.domain.entity;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A birthday gift given to a participant. One per person per calendar year — see the unique constraint in schema. */
public record BirthdayGift(
        Long id,
        long personId,
        int year,
        LocalDate giftDate,
        Money amount,
        PaymentMethod paymentMethod,
        String notes,
        LocalDateTime createdAt) {

    public BirthdayGift {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Ingresa un valor mayor a $0.");
        }
        if (giftDate == null) {
            throw new IllegalArgumentException("Selecciona la fecha del regalo.");
        }
        if (giftDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del regalo no puede ser futura.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Selecciona el medio de pago.");
        }
        notes = (notes == null || notes.isBlank()) ? null : notes.trim();
    }

    public static BirthdayGift create(long personId, LocalDate giftDate, Money amount, PaymentMethod paymentMethod, String notes) {
        return new BirthdayGift(null, personId, giftDate.getYear(), giftDate, amount, paymentMethod, notes, null);
    }
}
