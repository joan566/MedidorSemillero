package com.playground.fondoahorro.domain.savings;

/** A saving together with the owning person's name, for the general Ahorros ledger screen. */
public record SavingListItem(Saving saving, String personName) {
}
