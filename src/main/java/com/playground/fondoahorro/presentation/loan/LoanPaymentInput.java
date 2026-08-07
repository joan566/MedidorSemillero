package com.playground.fondoahorro.presentation.loan;

import com.playground.fondoahorro.domain.movement.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanPaymentInput(BigDecimal amount, LocalDate date, PaymentMethod method, String notes) {
}
