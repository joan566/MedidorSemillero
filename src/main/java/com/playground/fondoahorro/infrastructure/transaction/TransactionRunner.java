package com.playground.fondoahorro.infrastructure.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import com.playground.fondoahorro.infrastructure.exception.DataAccessException;
import com.playground.fondoahorro.infrastructure.config.DatabaseManager;

/**
 * Runs a block of repository calls against a single JDBC connection with
 * autocommit off, committing on success and rolling back on any failure —
 * this is how, e.g., registering a saving writes both the saving row and its
 * movement atomically (see project rule: an entity and its movement must
 * never be persisted independently).
 */
public final class TransactionRunner {

    private TransactionRunner() {
    }

    public static <T> T run(TransactionalAction<T> action) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = action.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException e) {
                connection.rollback();
                throw new DataAccessException("No fue posible completar la operación.", e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No fue posible completar la operación.", e);
        }
    }

    public static void runVoid(TransactionalVoidAction action) {
        run(connection -> {
            action.execute(connection);
            return null;
        });
    }

    @FunctionalInterface
    public interface TransactionalAction<T> {
        T execute(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionalVoidAction {
        void execute(Connection connection) throws SQLException;
    }
}
