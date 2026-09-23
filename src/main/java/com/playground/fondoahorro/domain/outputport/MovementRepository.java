package com.playground.fondoahorro.domain.outputport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.playground.fondoahorro.domain.entity.Movement;
import com.playground.fondoahorro.domain.vo.MovementFilter;
import com.playground.fondoahorro.domain.vo.MovementListItem;
import com.playground.fondoahorro.domain.vo.FundBalances;

public interface MovementRepository {

    /**
     * Inserts a movement as part of a caller-managed transaction (see
     * infrastructure.database.TransactionRunner). Always called alongside
     * the insert of the entity that produced it (saving, loan, payment,
     * gift, settlement), never on its own.
     */
    Movement insert(Connection connection, Movement movement) throws SQLException;

    List<MovementListItem> findAll(MovementFilter filter);

    FundBalances getBalances();

    /**
     * Same data as getBalances(), but read through the caller's transaction
     * connection so a balance check right before an insert can't race a
     * separate connection.
     */
    FundBalances getBalances(Connection connection) throws SQLException;

    Optional<Movement> findByReference(Connection connection, String referenceTable, long referenceId) throws SQLException;

    void update(Connection connection, Movement movement) throws SQLException;
}
