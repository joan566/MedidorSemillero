package com.playground.fondoahorro.domain.settlement;

import com.playground.fondoahorro.domain.person.Person;

import java.time.LocalDateTime;

/**
 * A person's settlement for a given year, always freshly calculated from
 * that year's savings (so the administrator sees the number without having
 * to prepare anything first), together with whether it has been prepared
 * (saved as a permanent record) and when.
 */
public record SettlementRow(Person person, Settlement calculated, boolean prepared, LocalDateTime preparedAt) {
}
