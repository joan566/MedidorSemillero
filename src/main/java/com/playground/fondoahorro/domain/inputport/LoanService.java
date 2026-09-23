package com.playground.fondoahorro.domain.inputport;

import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.entity.LoanInterestCharge;
import com.playground.fondoahorro.domain.entity.LoanPayment;
import com.playground.fondoahorro.domain.enums.LoanStatus;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.vo.LoanListItem;
import com.playground.fondoahorro.domain.vo.Money;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Interest accrues monthly on the outstanding principal balance (not a flat
 * amount computed once at creation), and there's no background scheduler in
 * this desktop app — so reading a loan (findById/list/historyForPerson) can
 * itself write to the database: if one or more monthly charges are overdue,
 * they're generated on the way out so the caller always sees today's real
 * numbers. This only writes when something is actually due; it's a no-op
 * (and no transaction is opened) otherwise.
 */
public interface LoanService {

    int defaultInterestRateBps();

    Loan createLoan(long personId, Money principalAmount, int interestRateBps, LocalDate loanDate,
                     PaymentMethod paymentMethod, String notes);

    /** Same as createLoan(...), but runs on a caller-supplied connection — used by the Excel importer to replay historical loans inside one shared transaction. */
    Loan createLoan(Connection connection, long personId, Money principalAmount, int interestRateBps, LocalDate loanDate,
                     PaymentMethod paymentMethod, String notes) throws SQLException;

    LoanPayment registerPayment(long loanId, Money amount, LocalDate paymentDate, PaymentMethod method, String notes);

    /** Same as registerPayment(...), but runs on a caller-supplied connection — used by the Excel importer to replay historical payments inside one shared transaction. */
    LoanPayment registerPayment(Connection connection, long loanId, Money amount, LocalDate paymentDate,
                                 PaymentMethod method, String notes) throws SQLException;

    Optional<Loan> findById(long id);

    List<Loan> historyForPerson(long personId);

    List<LoanListItem> list(LoanStatus statusFilter, Long personId);

    List<LoanPayment> paymentsForLoan(long loanId);

    List<LoanInterestCharge> chargesForLoan(long loanId);
}
