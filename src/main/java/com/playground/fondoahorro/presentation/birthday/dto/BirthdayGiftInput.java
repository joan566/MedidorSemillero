package com.playground.fondoahorro.presentation.birthday.dto;

import com.playground.fondoahorro.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BirthdayGiftInput(BigDecimal amount, LocalDate date, PaymentMethod method, String notes) {
}
