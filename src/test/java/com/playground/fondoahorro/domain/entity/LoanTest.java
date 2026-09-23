package com.playground.fondoahorro.domain.entity;

import com.playground.fondoahorro.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.playground.fondoahorro.domain.enums.LoanStatus;

class LoanTest {

    private static Money pesos(String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    void createStartsWithFullPrincipalAsBalanceAndNothingAccruedYet() {
        LocalDate loanDate = LocalDate.now();
        Loan loan = Loan.create(1L, pesos("1000000"), 300, loanDate, null);

        assertEquals(pesos("1000000"), loan.principalAmount());
        assertEquals(pesos("1000000"), loan.principalBalance());
        assertEquals(Money.ZERO, loan.interestOwed());
        assertEquals(Money.ZERO, loan.paidAmount());
        assertEquals(loanDate.plusMonths(1), loan.nextAccrualDate());
        assertEquals(LoanStatus.ACTIVE, loan.status());
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
    void monthlyInterestChargeIsThreePercentOfThePrincipalBalance() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null);

        assertEquals(pesos("30000"), loan.monthlyInterestCharge());
    }

    @Test
    void monthlyInterestChargeIsZeroWhenRateIsZero() {
        Loan loan = Loan.create(1L, pesos("500000"), 0, LocalDate.now(), null);

        assertEquals(Money.ZERO, loan.monthlyInterestCharge());
    }

    @Test
    void monthlyInterestChargeShrinksAsThePrincipalBalanceShrinks() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null);
        Loan accrued = loan.withAccrual(loan.monthlyInterestCharge()); // interestOwed = 30000, principalBalance unchanged
        Loan afterAbono = accrued.withPayment(pesos("530000")); // 30000 to interest, 500000 to principal

        assertEquals(pesos("500000"), afterAbono.principalBalance());
        assertEquals(pesos("15000"), afterAbono.monthlyInterestCharge(), "3% of the new, lower balance");
    }

    @Test
    void withAccrualMovesTheChargeIntoInterestOwedAndAdvancesNextAccrualDateByOneMonth() {
        LocalDate loanDate = LocalDate.now();
        Loan loan = Loan.create(1L, pesos("1000000"), 300, loanDate, null);

        Loan accrued = loan.withAccrual(loan.monthlyInterestCharge());

        assertEquals(pesos("30000"), accrued.interestOwed());
        assertEquals(pesos("1000000"), accrued.principalBalance(), "accrual never touches principal");
        assertEquals(loanDate.plusMonths(2), accrued.nextAccrualDate());
    }

    /**
     * The rule confirmed explicitly for this project: unpaid interest never
     * capitalizes. Two months missed in a row must charge 3% of the same
     * principal balance both times, not 3% of a growing (principal + unpaid
     * interest) figure.
     */
    @Test
    void unpaidInterestDoesNotCapitalizeIntoTheNextMonthsCharge() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null);

        Loan afterMonth1 = loan.withAccrual(loan.monthlyInterestCharge());
        Loan afterMonth2 = afterMonth1.withAccrual(afterMonth1.monthlyInterestCharge());

        assertEquals(pesos("1000000"), afterMonth2.principalBalance());
        assertEquals(pesos("60000"), afterMonth2.interestOwed(), "30000 + 30000, not compounded");
    }

    @Test
    void paymentSmallerThanInterestOwedReducesInterestOwedAndLeavesPrincipalBalanceUntouched() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("30000"));

        Loan afterPayment = loan.withPayment(pesos("10000"));

        assertEquals(pesos("20000"), afterPayment.interestOwed());
        assertEquals(pesos("1000000"), afterPayment.principalBalance());
        assertEquals(LoanStatus.ACTIVE, afterPayment.status());
    }

    @Test
    void paymentExceedingInterestOwedAppliesTheRemainderAsAbonoACapital() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("30000"));

        Loan afterPayment = loan.withPayment(pesos("80000"));

        assertEquals(Money.ZERO, afterPayment.interestOwed());
        assertEquals(pesos("950000"), afterPayment.principalBalance());
        assertEquals(pesos("80000"), afterPayment.paidAmount());
        assertEquals(LoanStatus.ACTIVE, afterPayment.status());
    }

    @Test
    void payingExactlyTheTotalOwedClosesTheLoan() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("30000"));

        Loan afterPayment = loan.withPayment(pesos("1030000"));

        assertEquals(Money.ZERO, afterPayment.principalBalance());
        assertEquals(Money.ZERO, afterPayment.interestOwed());
        assertEquals(LoanStatus.PAID, afterPayment.status());
    }

    @Test
    void sequentialPaymentsThatSumToTheTotalOwedCloseTheLoan() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("30000"));

        Loan afterFirst = loan.withPayment(pesos("530000"));
        Loan afterSecond = afterFirst.withPayment(pesos("500000"));

        assertEquals(Money.ZERO, afterSecond.totalOwed());
        assertEquals(LoanStatus.PAID, afterSecond.status());
    }

    /**
     * The rule confirmed explicitly for this project: a payment can never
     * exceed what is still owed. This is the single most important guard in
     * the whole loan module.
     */
    @Test
    void paymentLargerThanTotalOwedIsRejected() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("30000"));
        Loan afterFirstPayment = loan.withPayment(pesos("500000")); // totalOwed now 530000

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> afterFirstPayment.withPayment(pesos("530001")));
        assertTrue(exception.getMessage().toLowerCase().contains("pendiente"));

        // and the loan itself, being immutable, is provably untouched by the rejected attempt
        assertEquals(pesos("530000"), afterFirstPayment.totalOwed());
        assertEquals(LoanStatus.ACTIVE, afterFirstPayment.status());
    }

    @Test
    void paymentOfExactlyOnePesoMoreThanTotalOwedIsRejected() {
        Loan loan = Loan.create(1L, pesos("100000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("3000")); // totalOwed 103000

        assertThrows(IllegalArgumentException.class, () -> loan.withPayment(pesos("103000.01")));
    }

    @Test
    void nonPositivePaymentIsRejected() {
        Loan loan = Loan.create(1L, pesos("100000"), 300, LocalDate.now(), null);

        assertThrows(IllegalArgumentException.class, () -> loan.withPayment(Money.ZERO));
        assertThrows(IllegalArgumentException.class, () -> loan.withPayment(pesos("-1")));
    }

    @Test
    void previewPaymentMatchesTheAllocationThatWithPaymentActuallyApplies() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("30000"));

        Loan.PaymentAllocation preview = loan.previewPayment(pesos("80000"));
        Loan afterPayment = loan.withPayment(pesos("80000"));

        assertEquals(preview.interestPortion(), pesos("30000"));
        assertEquals(preview.principalPortion(), pesos("50000"));
        assertEquals(loan.principalBalance().minus(preview.principalPortion()), afterPayment.principalBalance());
        assertEquals(loan.interestOwed().minus(preview.interestPortion()), afterPayment.interestOwed());
    }

    @Test
    void previewPaymentOfAnAmountLargerThanTotalOwedClampsRatherThanGoingNegative() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("30000")); // totalOwed 1030000

        Loan.PaymentAllocation preview = loan.previewPayment(pesos("9999999"));

        assertEquals(pesos("30000"), preview.interestPortion());
        assertEquals(pesos("1000000"), preview.principalPortion(), "clamped to what's actually left in principal");
    }

    @Test
    void totalOwedIsPrincipalBalancePlusInterestOwed() {
        Loan loan = Loan.create(1L, pesos("1000000"), 300, LocalDate.now(), null)
                .withAccrual(pesos("30000"));

        assertEquals(pesos("1030000"), loan.totalOwed());
    }
}
