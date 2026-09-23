package com.playground.fondoahorro.domain.vo;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.entity.Person;

/**
 * A person together with the financial totals shown in the persons list and
 * detail screen. totalSavings and outstandingDebt are computed straight from
 * the savings/loans ledger, never stored.
 */
public record PersonSummary(Person person, Money totalSavings, Money outstandingDebt) {
}
