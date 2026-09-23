package com.playground.fondoahorro.presentation.loan.dto;

import com.playground.fondoahorro.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanInput(long personId, String personName, BigDecimal amount, int interestRateBps,
                         LocalDate date, PaymentMethod method, String notes) {
}
