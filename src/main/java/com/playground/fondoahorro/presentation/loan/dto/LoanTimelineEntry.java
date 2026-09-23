package com.playground.fondoahorro.presentation.loan.dto;

import com.playground.fondoahorro.domain.entity.LoanInterestCharge;
import com.playground.fondoahorro.domain.entity.LoanPayment;

import java.time.LocalDate;

/** Presentation-only union of a loan's payments and interest charges, merged into one chronological timeline. */
public sealed interface LoanTimelineEntry {

    LocalDate date();

    record PaymentEntry(LoanPayment payment) implements LoanTimelineEntry {
        @Override
        public LocalDate date() {
            return payment.paymentDate();
        }
    }

    record ChargeEntry(LoanInterestCharge charge) implements LoanTimelineEntry {
        @Override
        public LocalDate date() {
            return charge.dueDate();
        }
    }
}
