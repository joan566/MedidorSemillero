package com.playground.fondoahorro.domain.savings;

import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SavingTest {

    @Test
    void createBuildsAValidSaving() {
        Saving saving = Saving.create(1L, Money.of(new BigDecimal("250000")), LocalDate.now(), PaymentMethod.CASH, "Ahorro enero");

        assertEquals(Money.of(new BigDecimal("250000")), saving.amount());
        assertEquals(PaymentMethod.CASH, saving.paymentMethod());
        assertEquals("Ahorro enero", saving.notes());
    }

    @Test
    void blankNotesAreNormalizedToNull() {
        Saving saving = Saving.create(1L, Money.of(new BigDecimal("1000")), LocalDate.now(), PaymentMethod.CASH, "   ");
        assertNull(saving.notes());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Saving.create(1L, Money.ZERO, LocalDate.now(), PaymentMethod.CASH, null));
    }

    @Test
    void rejectsFutureDate() {
        assertThrows(IllegalArgumentException.class,
                () -> Saving.create(1L, Money.of(new BigDecimal("1000")), LocalDate.now().plusDays(1), PaymentMethod.CASH, null));
    }

    @Test
    void rejectsMissingPaymentMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> Saving.create(1L, Money.of(new BigDecimal("1000")), LocalDate.now(), null, null));
    }
}
