package com.playground.fondoahorro.domain.outputport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.enums.LoanStatus;
import com.playground.fondoahorro.domain.vo.LoanListItem;

public interface LoanRepository {

    Loan insert(Connection connection, Loan loan) throws SQLException;

    /** Persists principalBalance/interestOwed/paidAmount/nextAccrualDate/status after an accrual or a payment. */
    void updateBalance(Connection connection, Loan loan) throws SQLException;

    Optional<Loan> findById(long id);

    /** Same as findById(long), but on a caller-supplied connection — needed to see a loan inserted earlier in the same uncommitted transaction. */
    Optional<Loan> findById(Connection connection, long id) throws SQLException;

    List<Loan> findByPerson(long personId);

    /** statusFilter null means "Todos". personId null means no filter on person. */
    List<LoanListItem> findAll(LoanStatus statusFilter, Long personId);
}
