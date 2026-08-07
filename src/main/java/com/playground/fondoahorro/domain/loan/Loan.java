package com.playground.fondoahorro.domain.loan;

import com.playground.fondoahorro.domain.money.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A loan made to a participant. interestAmount/totalAmount/outstandingAmount
 * are always derived, never entered by hand — the administrator should never
 * need a calculator to know what someone owes.
 */
public record Loan(
        Long id,
        long personId,
        Money principalAmount,
        int interestRateBps,
        Money interestAmount,
        Money totalAmount,
        Money paidAmount,
        Money outstandingAmount,
        LocalDate loanDate,
        LoanStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public Loan {
        if (principalAmount == null || !principalAmount.isPositive()) {
            throw new IllegalArgumentException("El monto del préstamo debe ser mayor a $0.");
        }
        if (interestRateBps < 0) {
            throw new IllegalArgumentException("La tasa de interés no puede ser negativa.");
        }
        if (loanDate == null) {
            throw new IllegalArgumentException("Selecciona la fecha del préstamo.");
        }
        if (loanDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del préstamo no puede ser futura.");
        }
        notes = (notes == null || notes.isBlank()) ? null : notes.trim();
    }

    public static Loan create(long personId, Money principalAmount, int interestRateBps, LocalDate loanDate, String notes) {
        if (principalAmount == null || !principalAmount.isPositive()) {
            throw new IllegalArgumentException("El monto del préstamo debe ser mayor a $0.");
        }
        Money interestAmount = principalAmount.multiply(rateFromBps(interestRateBps));
        Money totalAmount = principalAmount.plus(interestAmount);
        return new Loan(null, personId, principalAmount, interestRateBps, interestAmount, totalAmount,
                Money.ZERO, totalAmount, loanDate, LoanStatus.ACTIVE, notes, null, null);
    }

    /** Applies a payment, returning the updated loan. Rejects a payment larger than what's still owed. */
    public Loan withPayment(Money paymentAmount) {
        if (paymentAmount == null || !paymentAmount.isPositive()) {
            throw new IllegalArgumentException("Ingresa un valor mayor a $0.");
        }
        if (paymentAmount.isGreaterThan(outstandingAmount)) {
            throw new IllegalArgumentException("El pago no puede ser mayor a la deuda pendiente.");
        }
        Money newPaid = paidAmount.plus(paymentAmount);
        Money newOutstanding = outstandingAmount.minus(paymentAmount);
        LoanStatus newStatus = newOutstanding.isZero() ? LoanStatus.PAID : LoanStatus.ACTIVE;
        return new Loan(id, personId, principalAmount, interestRateBps, interestAmount, totalAmount,
                newPaid, newOutstanding, loanDate, newStatus, notes, createdAt, updatedAt);
    }

    private static BigDecimal rateFromBps(int bps) {
        return BigDecimal.valueOf(bps).movePointLeft(4);
    }
}
