package com.playground.fondoahorro.domain.movement;

import com.playground.fondoahorro.domain.money.Money;

/** Cash/transfer/total balances for both funds, always derived from the movement ledger. */
public record FundBalances(Money savingsCash, Money savingsTransfer, Money birthdayCash, Money birthdayTransfer) {

    public static FundBalances empty() {
        return new FundBalances(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);
    }

    public Money savingsTotal() {
        return savingsCash.plus(savingsTransfer);
    }

    public Money birthdayTotal() {
        return birthdayCash.plus(birthdayTransfer);
    }

    public Money balanceFor(Fund fund, PaymentMethod method) {
        return switch (fund) {
            case SAVINGS -> method == PaymentMethod.CASH ? savingsCash : savingsTransfer;
            case BIRTHDAY -> method == PaymentMethod.CASH ? birthdayCash : birthdayTransfer;
        };
    }

    public boolean hasSufficientBalance(Fund fund, PaymentMethod method, Money amount) {
        return !amount.isGreaterThan(balanceFor(fund, method));
    }
}
