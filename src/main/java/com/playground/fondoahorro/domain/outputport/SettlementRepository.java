package com.playground.fondoahorro.domain.outputport;

import java.util.List;
import java.util.Optional;
import com.playground.fondoahorro.domain.entity.Settlement;

public interface SettlementRepository {

    /** Inserts or, if one already exists for that person/year, replaces it with the freshly calculated values. */
    Settlement upsert(Settlement settlement);

    Optional<Settlement> findByPersonAndYear(long personId, int year);

    List<Settlement> findByPerson(long personId);
}
