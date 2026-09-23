package com.playground.fondoahorro.domain.entity;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanPayment(
        Long id,
        long loanId,
        Money amount,
        Money interestPortion,
        Money principalPortion,
        LocalDate paymentDate,
        PaymentMethod paymentMethod,
        String notes,
        LocalDateTime createdAt) {

    public LoanPayment {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Ingresa un valor mayor a $0.");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("Selecciona la fecha del pago.");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del pago no puede ser futura.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Selecciona el medio de pago.");
        }
        notes = (notes == null || notes.isBlank()) ? null : notes.trim();
    }

    public static LoanPayment create(long loanId, Money amount, Money interestPortion, Money principalPortion,
                                      LocalDate paymentDate, PaymentMethod paymentMethod, String notes) {
        return new LoanPayment(null, loanId, amount, interestPortion, principalPortion, paymentDate, paymentMethod, notes, null);
    }
}
