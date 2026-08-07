package com.playground.fondoahorro.domain.movement;

/** A movement together with the display names of its category and (optional) person, for the ledger screen. */
public record MovementListItem(Movement movement, String movementTypeName, String personName) {
}
