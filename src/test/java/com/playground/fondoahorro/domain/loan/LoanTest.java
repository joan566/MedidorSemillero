package com.playground.fondoahorro.domain.loan;

import com.playground.fondoahorro.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanTest {

    private static Money pesos(String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    void createComputesInterestAtTheDefaultThreePercentRate() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null);

        assertEquals(pesos("30000"), loan.interestAmount());
        assertEquals(pesos("1030000"), loan.totalAmount());
        assertEquals(pesos("1030000"), loan.outstandingAmount());
        assertEquals(Money.ZERO, loan.paidAmount());
        assertEquals(LoanStatus.ACTIVE, loan.status());
    }

    @Test
    void createComputesInterestAtOtherRates() {
        Loan loan = Loan.create(1L, pesos("200000"), 500, LocalDate.now(), null);

        assertEquals(pesos("10000"), loan.interestAmount());
        assertEquals(pesos("210000"), loan.totalAmount());
    }

    @Test
    void createWithZeroInterestRateChargesNoInterest() {
        Loan loan = Loan.create(1L, pesos("500000"), 0, LocalDate.now(), null);

        assertEquals(Money.ZERO, loan.interestAmount());
        assertEquals(pesos("500000"), loan.totalAmount());
    }

    @Test
    void createRejectsNonPositivePrincipal() {
        assertThrows(IllegalArgumentException.class,
                () -> Loan.create(1L, Money.ZERO, 300, LocalDate.now(), null));
        assertThrows(IllegalArgumentException.class,
                () -> Loan.create(1L, pesos("-1"), 300, LocalDate.now(), null));
    }

    @Test
    void createRejectsNullPrincipalWithAFriendlyMessageRatherThanNpe() {
        assertThrows(IllegalArgumentException.class,
                () -> Loan.create(1L, null, 300, LocalDate.now(), null));
    }

    @Test
    void createRejectsNegativeInterestRate() {
        assertThrows(IllegalArgumentException.class,
                () -> Loan.create(1L, pesos("100000"), -1, LocalDate.now(), null));
    }

    @Test
    void createRejectsFutureLoanDate() {
        assertThrows(IllegalArgumentException.class,
                () -> Loan.create(1L, pesos("100000"), 300, LocalDate.now().plusDays(1), null));
    }

    @Test
    void partialPaymentReducesOutstandingAndKeepsLoanActive() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null);

        Loan afterPayment = loan.withPayment(pesos("500000"));

        assertEquals(pesos("500000"), afterPayment.paidAmount());
        assertEquals(pesos("530000"), afterPayment.outstandingAmount());
        assertEquals(LoanStatus.ACTIVE, afterPayment.status());
    }

    @Test
    void payingExactlyTheOutstandingAmountClosesTheLoan() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null);

        Loan afterPayment = loan.withPayment(pesos("1030000"));

        assertEquals(Money.ZERO, afterPayment.outstandingAmount());
        assertEquals(pesos("1030000"), afterPayment.paidAmount());
        assertEquals(LoanStatus.PAID, afterPayment.status());
    }

    @Test
    void sequentialPaymentsThatSumToTheOutstandingAmountClosesTheLoan() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null);

        Loan afterFirst = loan.withPayment(pesos("530000"));
        Loan afterSecond = afterFirst.withPayment(pesos("500000"));

        assertEquals(Money.ZERO, afterSecond.outstandingAmount());
        assertEquals(LoanStatus.PAID, afterSecond.status());
    }

    /**
     * The rule confirmed explicitly for this project: a payment can never
     * exceed what is still owed. This is the single most important guard in
     * the whole loan module.
     */
    @Test
    void paymentLargerThanOutstandingIsRejected() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null);
        Loan afterFirstPayment = loan.withPayment(pesos("500000")); // outstanding now 530000

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> afterFirstPayment.withPayment(pesos("530001")));
        assertTrue(exception.getMessage().toLowerCase().contains("pendiente"));

        // and the loan itself, being immutable, is provably untouched by the rejected attempt
        assertEquals(pesos("530000"), afterFirstPayment.outstandingAmount());
        assertEquals(LoanStatus.ACTIVE, afterFirstPayment.status());
    }

    @Test
    void paymentOfExactlyOnePesoMoreThanOutstandingIsRejected() {
        Loan loan = Loan.create(1L, pesos("100000"), 300, LocalDate.now(), null); // outstanding 103000

        assertThrows(IllegalArgumentException.class, () -> loan.withPayment(pesos("103000.01")));
    }

    @Test
    void nonPositivePaymentIsRejected() {
        Loan loan = Loan.create(1L, pesos("100000"), 300, LocalDate.now(), null);

        assertThrows(IllegalArgumentException.class, () -> loan.withPayment(Money.ZERO));
        assertThrows(IllegalArgumentException.class, () -> loan.withPayment(pesos("-1")));
    }
}
