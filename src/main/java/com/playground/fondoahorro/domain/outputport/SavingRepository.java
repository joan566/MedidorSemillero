package com.playground.fondoahorro.domain.outputport;

import com.playground.fondoahorro.domain.vo.Money;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.playground.fondoahorro.domain.entity.Saving;
import com.playground.fondoahorro.domain.vo.SavingListItem;

public interface SavingRepository {

    /** Called from within a transaction alongside the matching movement insert (see TransactionRunner). */
    Saving insert(Connection connection, Saving saving) throws SQLException;

    Optional<Saving> findById(long id);

    /** Called from within a transaction alongside the matching movement update (see TransactionRunner). */
    Saving update(Connection connection, Saving saving) throws SQLException;

    List<Saving> findByPerson(long personId);

    /** personId null means no filter (all persons). */
    List<SavingListItem> findAll(Long personId);

    /** Sum of savings recorded for that person within that calendar year — the basis for a settlement. */
    Money totalForPersonAndYear(long personId, int year);
}
