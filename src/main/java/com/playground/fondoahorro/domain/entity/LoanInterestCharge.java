package com.playground.fondoahorro.domain.entity;

import com.playground.fondoahorro.domain.vo.Money;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.playground.fondoahorro.domain.enums.LoanInterestChargeStatus;

/** One month's interest charge on a loan — the ledger row behind interestOwed. */
public record LoanInterestCharge(
        Long id,
        long loanId,
        LocalDate dueDate,
        Money principalBalance,
        Money interestAmount,
        Money paidAmount,
        LoanInterestChargeStatus status,
        LocalDateTime createdAt) {

    public static LoanInterestCharge create(long loanId, LocalDate dueDate, Money principalBalance, Money interestAmount) {
        return new LoanInterestCharge(null, loanId, dueDate, principalBalance, interestAmount, Money.ZERO,
                LoanInterestChargeStatus.PENDING, null);
    }

    /** Applies part (or all) of a payment to this charge, returning the updated charge. */
    public LoanInterestCharge withPayment(Money amount) {
        Money newPaid = paidAmount.plus(amount);
        LoanInterestChargeStatus newStatus = newPaid.isGreaterThan(interestAmount) || newPaid.equals(interestAmount)
                ? LoanInterestChargeStatus.PAID
                : LoanInterestChargeStatus.PARTIAL;
        return new LoanInterestCharge(id, loanId, dueDate, principalBalance, interestAmount, newPaid, newStatus, createdAt);
    }
}
