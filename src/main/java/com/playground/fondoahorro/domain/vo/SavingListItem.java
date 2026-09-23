package com.playground.fondoahorro.domain.vo;

import com.playground.fondoahorro.domain.entity.Saving;
/** A saving together with the owning person's name, for the general Ahorros ledger screen. */
public record SavingListItem(Saving saving, String personName) {
}
