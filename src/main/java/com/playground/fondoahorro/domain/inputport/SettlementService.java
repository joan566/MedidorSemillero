package com.playground.fondoahorro.domain.inputport;

import com.playground.fondoahorro.domain.entity.Settlement;
import com.playground.fondoahorro.domain.vo.SettlementRow;

import java.util.List;
import java.util.Optional;

/**
 * Calculates and stores annual settlements. A settlement never moves money
 * on its own — no movement or transfer is created when one is prepared (see
 * project decision: preparing/consulting is a calculation aid, not a payout).
 */
public interface SettlementService {

    Settlement calculateForPerson(long personId, int year);

    List<SettlementRow> listForYear(int year);

    Settlement prepare(long personId, int year);

    Optional<Settlement> findPrepared(long personId, int year);

    List<Settlement> historyForPerson(long personId);
}
