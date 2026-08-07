package com.playground.fondoahorro.domain.settlement;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository {

    /** Inserts or, if one already exists for that person/year, replaces it with the freshly calculated values. */
    Settlement upsert(Settlement settlement);

    Optional<Settlement> findByPersonAndYear(long personId, int year);

    List<Settlement> findByPerson(long personId);
}
