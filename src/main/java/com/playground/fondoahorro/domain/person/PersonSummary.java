package com.playground.fondoahorro.domain.person;

import com.playground.fondoahorro.domain.money.Money;

/**
 * A person together with the financial totals shown in the persons list and
 * detail screen. totalSavings and outstandingDebt are computed straight from
 * the savings/loans ledger, never stored.
 */
public record PersonSummary(Person person, Money totalSavings, Money outstandingDebt) {
}
