package com.playground.fondoahorro.application.importing.service;

import com.playground.fondoahorro.domain.inputport.LoanService;
import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.inputport.SavingService;
import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.infrastructure.transaction.TransactionRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_LOANS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_PAYMENTS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_PERSONS;
import static com.playground.fondoahorro.application.importing.service.ExcelWorkbookParser.SHEET_SAVINGS;
import com.playground.fondoahorro.application.importing.dto.ImportPlan;
import com.playground.fondoahorro.application.importing.dto.ImportExecutionException;

/**
 * Executes an already-validated ImportPlan (see ImportValidator) inside a
 * single database transaction, reusing PersonService/SavingService/LoanService
 * exactly as the live app does — so an imported loan's principalBalance and
 * interestOwed end up computed by the same accrual/payment-allocation rules
 * as if every row had been typed into the app in real time, one by one, on
 * its own date. Persons are created first (in file order, since they don't
 * affect the fund balance); savings, loans and payments are then replayed
 * together in chronological order, because loan creation checks the fund's
 * balance at that point in its history and a payment can only be allocated
 * against interest that had actually accrued by its own date.
 */
public class ImportServiceImpl implements ImportService {

    private final PersonService personService;
    private final SavingService savingService;
    private final LoanService loanService;

    public ImportServiceImpl(PersonService personService, SavingService savingService, LoanService loanService) {
        this.personService = personService;
        this.savingService = savingService;
        this.loanService = loanService;
    }

    /** Expects a plan that came out of a successful ImportValidator.validate(...) — every cross-reference is assumed to resolve. */
    @Override
    public ImportSummary execute(ImportPlan plan) {
        return TransactionRunner.run(connection -> {
            Map<String, Long> personIdsByName = new HashMap<>();
            for (ImportPlan.PersonRow row : plan.persons()) {
                try {
                    Person created = personService.createPerson(connection, row.name(), row.birthDate(), row.phone());
                    personIdsByName.put(row.name(), created.id());
                } catch (RuntimeException e) {
                    throw new ImportExecutionException(SHEET_PERSONS, row.rowNumber(),
                            "No fue posible crear a \"" + row.name() + "\": " + e.getMessage());
                }
            }

            List<Operation> operations = new ArrayList<>();
            plan.savings().forEach(row -> operations.add(new SavingOperation(row)));
            plan.loans().forEach(row -> operations.add(new LoanOperation(row)));
            plan.payments().forEach(row -> operations.add(new PaymentOperation(row)));
            // Stable sort: on a tie, savings/loans/payments keep the relative order they
            // were added above, so a loan and its own payment on the same date never race.
            operations.sort(Comparator.comparing(Operation::date));

            Map<String, Long> loanIdsByLocalId = new HashMap<>();
            int savingsCreated = 0;
            int loansCreated = 0;
            int paymentsCreated = 0;
            for (Operation operation : operations) {
                switch (operation) {
                    case SavingOperation savingOp -> {
                        ImportPlan.SavingRow row = savingOp.row();
                        try {
                            savingService.registerSaving(connection, personIdsByName.get(row.personName()),
                                    row.amount(), row.date(), row.method(), row.notes());
                            savingsCreated++;
                        } catch (RuntimeException e) {
                            throw new ImportExecutionException(SHEET_SAVINGS, row.rowNumber(),
                                    "No fue posible registrar el ahorro de \"" + row.personName() + "\": " + e.getMessage());
                        }
                    }
                    case LoanOperation loanOp -> {
                        ImportPlan.LoanRow row = loanOp.row();
                        try {
                            Loan created = loanService.createLoan(connection, personIdsByName.get(row.personName()),
                                    row.principal(), row.interestRateBps(), row.loanDate(), row.method(), row.notes());
                            loanIdsByLocalId.put(row.localLoanId(), created.id());
                            loansCreated++;
                        } catch (RuntimeException e) {
                            throw new ImportExecutionException(SHEET_LOANS, row.rowNumber(),
                                    "No fue posible registrar el préstamo " + row.localLoanId() + ": " + e.getMessage());
                        }
                    }
                    case PaymentOperation paymentOp -> {
                        ImportPlan.PaymentRow row = paymentOp.row();
                        try {
                            loanService.registerPayment(connection, loanIdsByLocalId.get(row.localLoanId()),
                                    row.amount(), row.paymentDate(), row.method(), row.notes());
                            paymentsCreated++;
                        } catch (RuntimeException e) {
                            throw new ImportExecutionException(SHEET_PAYMENTS, row.rowNumber(),
                                    "No fue posible registrar el pago del préstamo " + row.localLoanId() + ": " + e.getMessage());
                        }
                    }
                }
            }
            return new ImportSummary(plan.persons().size(), savingsCreated, loansCreated, paymentsCreated);
        });
    }

    private sealed interface Operation permits SavingOperation, LoanOperation, PaymentOperation {
        LocalDate date();
    }

    private record SavingOperation(ImportPlan.SavingRow row) implements Operation {
        @Override
        public LocalDate date() {
            return row.date();
        }
    }

    private record LoanOperation(ImportPlan.LoanRow row) implements Operation {
        @Override
        public LocalDate date() {
            return row.loanDate();
        }
    }

    private record PaymentOperation(ImportPlan.PaymentRow row) implements Operation {
        @Override
        public LocalDate date() {
            return row.paymentDate();
        }
    }
}
