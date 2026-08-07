package com.playground.fondoahorro.domain.loan;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface LoanPaymentRepository {

    LoanPayment insert(Connection connection, LoanPayment payment) throws SQLException;

    List<LoanPayment> findByLoan(long loanId);
}
