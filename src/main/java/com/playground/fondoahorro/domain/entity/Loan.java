package com.playground.fondoahorro.domain.entity;

import com.playground.fondoahorro.domain.vo.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.playground.fondoahorro.domain.enums.LoanStatus;

/**
 * A loan made to a participant. Interest is not a fixed number baked in at
 * creation — principalBalance/interestOwed are live balances that move as
 * monthly interest accrues (see LoanService.accrueDueInterest) and as
 * payments are applied. interestOwed accumulates as a separate arrears
 * figure when a month's interest isn't paid; it is never folded into
 * principalBalance (no compounding).
 */
public record Loan(
        Long id,
        long personId,
        Money principalAmount,
        int interestRateBps,
        Money principalBalance,
        Money interestOwed,
        Money paidAmount,
        LocalDate loanDate,
        LocalDate nextAccrualDate,
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
        return new Loan(null, personId, principalAmount, interestRateBps, principalAmount, Money.ZERO,
                Money.ZERO, loanDate, loanDate.plusMonths(1), LoanStatus.ACTIVE, notes, null, null);
    }

    /** What's still owed today: unpaid capital plus accrued-but-unpaid interest. */
    public Money totalOwed() {
        return principalBalance.plus(interestOwed);
    }

    /** 3% (or whatever interestRateBps is) of the current principal balance — one month's charge. */
    public Money monthlyInterestCharge() {
        return principalBalance.multiply(rateFromBps(interestRateBps));
    }

    /** Adds one month's accrued charge to interestOwed and advances nextAccrualDate. Never touches principalBalance. */
    public Loan withAccrual(Money chargeAmount) {
        return new Loan(id, personId, principalAmount, interestRateBps, principalBalance, interestOwed.plus(chargeAmount),
                paidAmount, loanDate, nextAccrualDate.plusMonths(1), status, notes, createdAt, updatedAt);
    }

    /**
     * Applies a payment: covers interestOwed first, any remainder reduces
     * principalBalance (abono a capital). Rejects a payment larger than
     * what's still owed in total.
     */
    public Loan withPayment(Money paymentAmount) {
        if (paymentAmount == null || !paymentAmount.isPositive()) {
            throw new IllegalArgumentException("Ingresa un valor mayor a $0.");
        }
        if (paymentAmount.isGreaterThan(totalOwed())) {
            throw new IllegalArgumentException("El pago no puede ser mayor a la deuda pendiente.");
        }
        PaymentAllocation allocation = previewPayment(paymentAmount);
        Money newPaid = paidAmount.plus(paymentAmount);
        Money newInterestOwed = interestOwed.minus(allocation.interestPortion());
        Money newPrincipalBalance = principalBalance.minus(allocation.principalPortion());
        LoanStatus newStatus = newPrincipalBalance.isZero() && newInterestOwed.isZero() ? LoanStatus.PAID : LoanStatus.ACTIVE;
        return new Loan(id, personId, principalAmount, interestRateBps, newPrincipalBalance, newInterestOwed,
                newPaid, loanDate, nextAccrualDate, newStatus, notes, createdAt, updatedAt);
    }

    /**
     * Pure preview of how a payment would split between interest and
     * principal, without applying it. Reused by withPayment itself and by
     * the payment form's live breakdown — the allocation rule lives in one
     * place only.
     */
    public PaymentAllocation previewPayment(Money paymentAmount) {
        if (paymentAmount == null || !paymentAmount.isPositive()) {
            return new PaymentAllocation(Money.ZERO, Money.ZERO, interestOwed);
        }
        Money interestPortion = paymentAmount.isGreaterThan(interestOwed) ? interestOwed : paymentAmount;
        Money principalPortion = paymentAmount.minus(interestPortion);
        if (principalPortion.isGreaterThan(principalBalance)) {
            principalPortion = principalBalance;
        }
        return new PaymentAllocation(interestPortion, principalPortion, interestOwed.minus(interestPortion));
    }

    public record PaymentAllocation(Money interestPortion, Money principalPortion, Money remainingInterestOwed) {
    }

    private static BigDecimal rateFromBps(int bps) {
        return BigDecimal.valueOf(bps).movePointLeft(4);
    }
}
