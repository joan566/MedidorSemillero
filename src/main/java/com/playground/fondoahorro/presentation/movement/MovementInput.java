package com.playground.fondoahorro.presentation.movement;

import com.playground.fondoahorro.domain.movement.Fund;
import com.playground.fondoahorro.domain.movement.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovementInput(long movementTypeId, Fund fund, Long personId, BigDecimal amount, LocalDate date,
                             PaymentMethod method, String notes) {
}
