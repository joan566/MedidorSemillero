package com.playground.fondoahorro.domain.entity;

import com.playground.fondoahorro.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.enums.MovementKind;

class MovementTest {

    @Test
    void createBuildsAValidMovement() {
        Movement movement = Movement.create(1L, Fund.SAVINGS, PaymentMethod.CASH, MovementKind.INCOME,
                Money.of(new BigDecimal("100000")), LocalDate.now(), 5L, "savings", 10L, "nota");

        assertEquals(Fund.SAVINGS, movement.fund());
        assertEquals(MovementKind.INCOME, movement.kind());
        assertEquals(Money.of(new BigDecimal("100000")), movement.amount());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> Movement.create(1L, Fund.SAVINGS, PaymentMethod.CASH,
                MovementKind.INCOME, Money.ZERO, LocalDate.now(), null, null, null, null));
    }

    @Test
    void rejectsMissingFund() {
        assertThrows(IllegalArgumentException.class, () -> Movement.create(1L, null, PaymentMethod.CASH,
                MovementKind.INCOME, Money.of(new BigDecimal("1000")), LocalDate.now(), null, null, null, null));
    }

    @Test
    void rejectsMissingPaymentMethod() {
        assertThrows(IllegalArgumentException.class, () -> Movement.create(1L, Fund.SAVINGS, null,
                MovementKind.INCOME, Money.of(new BigDecimal("1000")), LocalDate.now(), null, null, null, null));
    }

    @Test
    void rejectsMissingKind() {
        assertThrows(IllegalArgumentException.class, () -> Movement.create(1L, Fund.SAVINGS, PaymentMethod.CASH,
                null, Money.of(new BigDecimal("1000")), LocalDate.now(), null, null, null, null));
    }
}
