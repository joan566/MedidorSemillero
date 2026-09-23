package com.playground.fondoahorro.presentation.savings.dto;

import com.playground.fondoahorro.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingInput(long personId, BigDecimal amount, LocalDate date, PaymentMethod method, String notes) {
}
