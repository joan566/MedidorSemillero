package com.playground.fondoahorro.domain.loan;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {

    Loan insert(Connection connection, Loan loan) throws SQLException;

    /** Persists paidAmount/outstandingAmount/status after a payment has been applied. */
    void updateBalance(Connection connection, Loan loan) throws SQLException;

    Optional<Loan> findById(long id);

    List<Loan> findByPerson(long personId);

    /** statusFilter null means "Todos". personId null means no filter on person. */
    List<LoanListItem> findAll(LoanStatus statusFilter, Long personId);
}
