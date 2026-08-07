package com.playground.fondoahorro.application.loan;

import com.playground.fondoahorro.application.settings.AppSettingsService;
import com.playground.fondoahorro.domain.loan.Loan;
import com.playground.fondoahorro.domain.loan.LoanListItem;
import com.playground.fondoahorro.domain.loan.LoanPayment;
import com.playground.fondoahorro.domain.loan.LoanPaymentRepository;
import com.playground.fondoahorro.domain.loan.LoanRepository;
import com.playground.fondoahorro.domain.loan.LoanStatus;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.Fund;
import com.playground.fondoahorro.domain.movement.FundBalances;
import com.playground.fondoahorro.domain.movement.Movement;
import com.playground.fondoahorro.domain.movement.MovementKind;
import com.playground.fondoahorro.domain.movement.MovementRepository;
import com.playground.fondoahorro.domain.movement.MovementType;
import com.playground.fondoahorro.domain.movement.MovementTypeRepository;
import com.playground.fondoahorro.domain.movement.PaymentMethod;
import com.playground.fondoahorro.domain.person.PersonRepository;
import com.playground.fondoahorro.infrastructure.database.TransactionRunner;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class LoanService {

    private static final String LOAN_DISBURSEMENT_CODE = "LOAN_DISBURSEMENT";
    private static final String LOAN_PAYMENT_CODE = "LOAN_PAYMENT";

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final MovementRepository movementRepository;
    private final MovementTypeRepository movementTypeRepository;
    private final PersonRepository personRepository;
    private final AppSettingsService appSettingsService;

    public LoanService(LoanRepository loanRepository, LoanPaymentRepository loanPaymentRepository,
                        MovementRepository movementRepository, MovementTypeRepository movementTypeRepository,
                        PersonRepository personRepository, AppSettingsService appSettingsService) {
        this.loanRepository = loanRepository;
        this.loanPaymentRepository = loanPaymentRepository;
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
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no existe."));
        MovementType type = movementTypeRepository.findByCode(LOAN_DISBURSEMENT_CODE)
                .orElseThrow(() -> new IllegalStateException("No se encontró el tipo de movimiento de préstamo."));

        Loan loan = Loan.create(personId, principalAmount, interestRateBps, loanDate, notes);

        return TransactionRunner.run(connection -> {
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
        });
    }

    public LoanPayment registerPayment(long loanId, Money amount, LocalDate paymentDate, PaymentMethod method, String notes) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("El préstamo no existe."));
        Loan updatedLoan = loan.withPayment(amount);
        MovementType type = movementTypeRepository.findByCode(LOAN_PAYMENT_CODE)
                .orElseThrow(() -> new IllegalStateException("No se encontró el tipo de movimiento de pago de préstamo."));

        LoanPayment payment = LoanPayment.create(loanId, amount, paymentDate, method, notes);

        return TransactionRunner.run(connection -> {
            LoanPayment inserted = loanPaymentRepository.insert(connection, payment);
            loanRepository.updateBalance(connection, updatedLoan);
            Movement movement = Movement.create(type.id(), Fund.SAVINGS, method, MovementKind.INCOME,
                    amount, paymentDate, loan.personId(), "loan_payments", inserted.id(), notes);
            movementRepository.insert(connection, movement);
            return inserted;
        });
    }

    public Optional<Loan> findById(long id) {
        return loanRepository.findById(id);
    }

    public List<Loan> historyForPerson(long personId) {
        return loanRepository.findByPerson(personId);
    }

    public List<LoanListItem> list(LoanStatus statusFilter, Long personId) {
        return loanRepository.findAll(statusFilter, personId);
    }

    public List<LoanPayment> paymentsForLoan(long loanId) {
        return loanPaymentRepository.findByLoan(loanId);
    }
}
