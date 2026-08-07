package com.playground.fondoahorro.presentation.savings;

import com.playground.fondoahorro.domain.movement.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingInput(long personId, BigDecimal amount, LocalDate date, PaymentMethod method, String notes) {
}
