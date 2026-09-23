package com.playground.fondoahorro.application.loan.service;

import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.inputport.LoanService;
import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.entity.LoanInterestCharge;
import com.playground.fondoahorro.domain.outputport.LoanInterestChargeRepository;
import com.playground.fondoahorro.domain.vo.LoanListItem;
import com.playground.fondoahorro.domain.entity.LoanPayment;
import com.playground.fondoahorro.domain.outputport.LoanPaymentRepository;
import com.playground.fondoahorro.domain.outputport.LoanRepository;
import com.playground.fondoahorro.domain.enums.LoanStatus;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.vo.FundBalances;
import com.playground.fondoahorro.domain.entity.Movement;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.outputport.MovementRepository;
import com.playground.fondoahorro.domain.entity.MovementType;
import com.playground.fondoahorro.domain.outputport.MovementTypeRepository;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.outputport.PersonRepository;
import com.playground.fondoahorro.infrastructure.transaction.TransactionRunner;

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
public class LoanServiceImpl implements LoanService {

    private static final String LOAN_DISBURSEMENT_CODE = "LOAN_DISBURSEMENT";
    private static final String LOAN_PAYMENT_CODE = "LOAN_PAYMENT";

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final LoanInterestChargeRepository loanInterestChargeRepository;
    private final MovementRepository movementRepository;
    private final MovementTypeRepository movementTypeRepository;
    private final PersonRepository personRepository;
    private final AppSettingsService appSettingsService;

    public LoanServiceImpl(LoanRepository loanRepository, LoanPaymentRepository loanPaymentRepository,
                        LoanInterestChargeRepository loanInterestChargeRepository, MovementRepository movementRepository,
                        MovementTypeRepository movementTypeRepository, PersonRepository personRepository,
                        AppSettingsService appSettingsService) {
        this.loanRepository = loanRepository;
        this.loanPaymentRepository = loanPaymentRepository;
        this.loanInterestChargeRepository = loanInterestChargeRepository;
        this.movementRepository = movementRepository;
        this.movementTypeRepository = movementTypeRepository;
        this.personRepository = personRepository;
        this.appSettingsService = appSettingsService;
    }

    public int defaultInterestRateBps() {
        return appSettingsService.getLoanInterestRateBps();
    }

    public Loan createLoan(long personId, Money principalAmount, int interestRateBps, LocalDate loanDate,
                            PaymentMethod paymentMethod, String notes) {
        return TransactionRunner.run(connection ->
                createLoan(connection, personId, principalAmount, interestRateBps, loanDate, paymentMethod, notes));
    }

    /** Same as createLoan(...), but runs on a caller-supplied connection — used by the Excel importer to replay historical loans inside one shared transaction. */
    public Loan createLoan(Connection connection, long personId, Money principalAmount, int interestRateBps, LocalDate loanDate,
                            PaymentMethod paymentMethod, String notes) throws SQLException {
        personRepository.findById(connection, personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no existe."));
        MovementType type = movementTypeRepository.findByCode(LOAN_DISBURSEMENT_CODE)
                .orElseThrow(() -> new IllegalStateException("No se encontró el tipo de movimiento de préstamo."));

        Loan loan = Loan.create(personId, principalAmount, interestRateBps, loanDate, notes);

        FundBalances balances = movementRepository.getBalances(connection);
        if (!balances.hasSufficientBalance(Fund.SAVINGS, paymentMethod, principalAmount)) {
            throw new IllegalArgumentException(
                    "El fondo de ahorros no tiene saldo suficiente para desembolsar este préstamo.");
        }
        Loan inserted = loanRepository.insert(connection, loan);
        Movement movement = Movement.create(type.id(), Fund.SAVINGS, paymentMethod, MovementKind.EXPENSE,
                principalAmount, loanDate, personId, "loans", inserted.id(), notes);
        movementRepository.insert(connection, movement);
        return inserted;
    }

    public LoanPayment registerPayment(long loanId, Money amount, LocalDate paymentDate, PaymentMethod method, String notes) {
        return TransactionRunner.run(connection -> registerPayment(connection, loanId, amount, paymentDate, method, notes));
    }

    /** Same as registerPayment(...), but runs on a caller-supplied connection — used by the Excel importer to replay historical payments inside one shared transaction. */
    public LoanPayment registerPayment(Connection connection, long loanId, Money amount, LocalDate paymentDate,
                                        PaymentMethod method, String notes) throws SQLException {
        Loan loan = loanRepository.findById(connection, loanId)
                .orElseThrow(() -> new IllegalArgumentException("El préstamo no existe."));
        MovementType type = movementTypeRepository.findByCode(LOAN_PAYMENT_CODE)
                .orElseThrow(() -> new IllegalStateException("No se encontró el tipo de movimiento de pago de préstamo."));

        // Accrued only up to paymentDate (not today) — a backdated payment must
        // compete only against the interest that was actually due by then, not
        // against interest that has accrued since. This is also what makes it
        // safe to replay a loan's full historical payment record in chronological
        // order (see the Excel importer) and get the exact same numbers as if the
        // app had been used in real time.
        Loan accrued = accrueDueInterest(connection, loan, paymentDate);
        Loan updatedLoan = accrued.withPayment(amount); // throws IllegalArgumentException if amount is invalid
        Loan.PaymentAllocation allocation = accrued.previewPayment(amount);

        applyChargePayments(connection, loanId, allocation.interestPortion());

        LoanPayment payment = LoanPayment.create(loanId, amount, allocation.interestPortion(),
                allocation.principalPortion(), paymentDate, method, notes);
        LoanPayment inserted = loanPaymentRepository.insert(connection, payment);
        loanRepository.updateBalance(connection, updatedLoan);
        Movement movement = Movement.create(type.id(), Fund.SAVINGS, method, MovementKind.INCOME,
                amount, paymentDate, loan.personId(), "loan_payments", inserted.id(), notes);
        movementRepository.insert(connection, movement);
        return inserted;
    }

    public Optional<Loan> findById(long id) {
        return loanRepository.findById(id).map(this::ensureAccrued);
    }

    public List<Loan> historyForPerson(long personId) {
        return loanRepository.findByPerson(personId).stream().map(this::ensureAccrued).toList();
    }

    public List<LoanListItem> list(LoanStatus statusFilter, Long personId) {
        return loanRepository.findAll(statusFilter, personId).stream()
                .map(item -> new LoanListItem(ensureAccrued(item.loan()), item.personName()))
                .toList();
    }

    public List<LoanPayment> paymentsForLoan(long loanId) {
        return loanPaymentRepository.findByLoan(loanId);
    }

    public List<LoanInterestCharge> chargesForLoan(long loanId) {
        return loanInterestChargeRepository.findByLoan(loanId);
    }

    private Loan ensureAccrued(Loan loan) {
        if (loan.status() == LoanStatus.PAID || loan.nextAccrualDate().isAfter(LocalDate.now())) {
            return loan;
        }
        return TransactionRunner.run(connection -> accrueDueInterest(connection, loan, LocalDate.now()));
    }

    /** Generates any loan_interest_charges due on or before asOfDate, folding each into interestOwed. */
    private Loan accrueDueInterest(Connection connection, Loan loan, LocalDate asOfDate) throws SQLException {
        Loan current = loan;
        while (current.status() != LoanStatus.PAID && !current.nextAccrualDate().isAfter(asOfDate)) {
            Money charge = current.monthlyInterestCharge();
            loanInterestChargeRepository.insert(connection, LoanInterestCharge.create(
                    current.id(), current.nextAccrualDate(), current.principalBalance(), charge));
            current = current.withAccrual(charge);
        }
        if (current != loan) {
            loanRepository.updateBalance(connection, current);
        }
        return current;
    }

    /** Marks the loan's oldest unpaid interest charges as covered, up to interestPortion. */
    private void applyChargePayments(Connection connection, long loanId, Money interestPortion) throws SQLException {
        Money remaining = interestPortion;
        for (LoanInterestCharge charge : loanInterestChargeRepository.findUnpaidByLoan(connection, loanId)) {
            if (!remaining.isPositive()) {
                break;
            }
            Money owedOnCharge = charge.interestAmount().minus(charge.paidAmount());
            Money portion = remaining.isGreaterThan(owedOnCharge) ? owedOnCharge : remaining;
            loanInterestChargeRepository.updatePayment(connection, charge.withPayment(portion));
            remaining = remaining.minus(portion);
        }
    }
}
