package com.playground.fondoahorro.presentation.loan.dto;

import com.playground.fondoahorro.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanPaymentInput(BigDecimal amount, LocalDate date, PaymentMethod method, String notes) {
}
