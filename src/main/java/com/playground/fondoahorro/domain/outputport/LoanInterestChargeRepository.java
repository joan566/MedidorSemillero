package com.playground.fondoahorro.domain.outputport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import com.playground.fondoahorro.domain.entity.LoanInterestCharge;

public interface LoanInterestChargeRepository {

    LoanInterestCharge insert(Connection connection, LoanInterestCharge charge) throws SQLException;

    /** Persists paidAmount/status after a payment has been allocated to this charge. */
    void updatePayment(Connection connection, LoanInterestCharge charge) throws SQLException;

    /** Unpaid/partially-paid charges for a loan, oldest due date first — used to allocate a payment across them. */
    List<LoanInterestCharge> findUnpaidByLoan(Connection connection, long loanId) throws SQLException;

    /** Full charge history for a loan, most recent first — for the loan detail screen. */
    List<LoanInterestCharge> findByLoan(long loanId);
}
