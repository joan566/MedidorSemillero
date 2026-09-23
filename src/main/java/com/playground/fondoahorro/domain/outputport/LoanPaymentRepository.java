package com.playground.fondoahorro.domain.outputport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import com.playground.fondoahorro.domain.entity.LoanPayment;

public interface LoanPaymentRepository {

    LoanPayment insert(Connection connection, LoanPayment payment) throws SQLException;

    List<LoanPayment> findByLoan(long loanId);
}
