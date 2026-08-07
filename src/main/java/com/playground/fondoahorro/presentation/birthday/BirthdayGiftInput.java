package com.playground.fondoahorro.presentation.birthday;

import com.playground.fondoahorro.domain.movement.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BirthdayGiftInput(BigDecimal amount, LocalDate date, PaymentMethod method, String notes) {
}
