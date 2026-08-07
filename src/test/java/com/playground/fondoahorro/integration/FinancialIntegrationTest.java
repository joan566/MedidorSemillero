package com.playground.fondoahorro.integration;

import com.playground.fondoahorro.application.birthday.BirthdayGiftService;
import com.playground.fondoahorro.application.loan.LoanService;
import com.playground.fondoahorro.application.movement.MovementService;
import com.playground.fondoahorro.application.person.PersonService;
import com.playground.fondoahorro.application.savings.SavingService;
import com.playground.fondoahorro.application.settings.AppSettingsService;
import com.playground.fondoahorro.application.settlement.SettlementService;
import com.playground.fondoahorro.domain.loan.Loan;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.Fund;
import com.playground.fondoahorro.domain.movement.FundBalances;
import com.playground.fondoahorro.domain.movement.Movement;
import com.playground.fondoahorro.domain.movement.MovementFilter;
import com.playground.fondoahorro.domain.movement.MovementKind;
import com.playground.fondoahorro.domain.movement.MovementRepository;
import com.playground.fondoahorro.domain.movement.MovementType;
import com.playground.fondoahorro.domain.movement.MovementTypeRepository;
import com.playground.fondoahorro.domain.movement.PaymentMethod;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.domain.savings.Saving;
import com.playground.fondoahorro.domain.settlement.Settlement;
import com.playground.fondoahorro.infrastructure.birthday.JdbcBirthdayGiftRepository;
import com.playground.fondoahorro.infrastructure.database.DatabaseManager;
import com.playground.fondoahorro.infrastructure.database.TransactionRunner;
import com.playground.fondoahorro.infrastructure.loan.JdbcLoanPaymentRepository;
import com.playground.fondoahorro.infrastructure.loan.JdbcLoanRepository;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementRepository;
import com.playground.fondoahorro.infrastructure.movement.JdbcMovementTypeRepository;
import com.playground.fondoahorro.infrastructure.person.JdbcPersonRepository;
import com.playground.fondoahorro.infrastructure.savings.JdbcSavingRepository;
import com.playground.fondoahorro.infrastructure.settings.JdbcAppSettingsRepository;
import com.playground.fondoahorro.infrastructure.settlement.JdbcSettlementRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises the financial rules that need a real database to prove: balances
 * derived from the movement ledger (section 42: "Saldos") and all-or-nothing
 * transactions (section 42: "Transacciones"). Runs against an isolated,
 * throwaway SQLite file under a JUnit temp directory — never the real
 * data/fondo-ahorro.db the application uses.
 */
class FinancialIntegrationTest {

    @TempDir
    static Path tempDir;

    private PersonService personService;
    private SavingService savingService;
    private LoanService loanService;
    private BirthdayGiftService birthdayGiftService;
    private MovementService movementService;
    private SettlementService settlementService;
    private MovementRepository movementRepository;
    private MovementTypeRepository movementTypeRepository;

    @BeforeAll
    static void initDatabase() {
        System.setProperty("fondoahorro.data.dir", tempDir.toString());
        DatabaseManager.initialize();
    }

    @BeforeEach
    void wireServices() {
        var personRepository = new JdbcPersonRepository();
        movementRepository = new JdbcMovementRepository();
        movementTypeRepository = new JdbcMovementTypeRepository();
        var savingRepository = new JdbcSavingRepository();
        var loanRepository = new JdbcLoanRepository();
        var loanPaymentRepository = new JdbcLoanPaymentRepository();
        var settlementRepository = new JdbcSettlementRepository();
        var giftRepository = new JdbcBirthdayGiftRepository();
        var appSettingsService = new AppSettingsService(new JdbcAppSettingsRepository());

        personService = new PersonService(personRepository);
        savingService = new SavingService(savingRepository, movementRepository, movementTypeRepository, personRepository);
        loanService = new LoanService(loanRepository, loanPaymentRepository, movementRepository, movementTypeRepository,
                personRepository, appSettingsService);
        birthdayGiftService = new BirthdayGiftService(giftRepository, movementRepository, movementTypeRepository,
                personRepository, appSettingsService);
        movementService = new MovementService(movementRepository, movementTypeRepository, personRepository);
        settlementService = new SettlementService(settlementRepository, savingRepository, personRepository, appSettingsService);
    }

    private static Money pesos(String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    void fundBalancesAreDerivedFromIncomeMinusExpensePerFundAndMethod() {
        // Deltas, not absolute totals: other @Test methods in this class share the
        // same database (only @BeforeAll initializes it), so an assertion on the
        // absolute balance would be polluted by whichever tests already ran.
        FundBalances before = movementService.getBalances();

        Person person = personService.createPerson("Test Saldos", LocalDate.of(1990, 1, 1), null);
        savingService.registerSaving(person.id(), pesos("500000"), LocalDate.now(), PaymentMethod.CASH, null);
        savingService.registerSaving(person.id(), pesos("300000"), LocalDate.now(), PaymentMethod.TRANSFER, null);
        loanService.createLoan(person.id(), pesos("100000"), 300, LocalDate.now(), PaymentMethod.CASH, null);

        FundBalances after = movementService.getBalances();

        assertEquals(pesos("400000"), after.savingsCash().minus(before.savingsCash()),
                "500.000 ahorrado - 100.000 prestado, ambos en efectivo");
        assertEquals(pesos("300000"), after.savingsTransfer().minus(before.savingsTransfer()));
        assertEquals(pesos("700000"), after.savingsTotal().minus(before.savingsTotal()));
    }

    @Test
    void loanPaymentPersistsTheUpdatedBalanceAndItsMovementTogether() {
        Person person = personService.createPerson("Test Transaccion Commit", LocalDate.of(1990, 1, 1), null);
        savingService.registerSaving(person.id(), pesos("100000"), LocalDate.now(), PaymentMethod.CASH, null);
        Loan loan = loanService.createLoan(person.id(), pesos("100000"), 300, LocalDate.now(), PaymentMethod.CASH, null);

        loanService.registerPayment(loan.id(), pesos("50000"), LocalDate.now(), PaymentMethod.CASH, null);

        Loan reloaded = loanService.findById(loan.id()).orElseThrow();
        assertEquals(pesos("50000"), reloaded.paidAmount());

        int movementsForPerson = movementService.list(new MovementFilter(null, null, null, null, null, person.id(), null)).size();
        assertEquals(3, movementsForPerson, "el aporte, el desembolso y el pago deben quedar juntos, todos registrados");
    }

    @Test
    void aTransactionThatFailsMidwayLeavesNoPartialWrite() {
        Person person = personService.createPerson("Test Rollback", LocalDate.of(1990, 1, 1), null);
        MovementType type = movementTypeRepository.findByCode("SAVINGS_CONTRIBUTION").orElseThrow();

        int before = movementService.list(MovementFilter.empty()).size();

        assertThrows(RuntimeException.class, () -> TransactionRunner.run(connection -> {
            Movement movement = Movement.create(type.id(), Fund.SAVINGS, PaymentMethod.CASH, MovementKind.INCOME,
                    pesos("999999"), LocalDate.now(), person.id(), null, null, "esto debe revertirse");
            movementRepository.insert(connection, movement);
            throw new RuntimeException("fallo simulado a mitad de la transacción");
        }));

        int after = movementService.list(MovementFilter.empty()).size();
        assertEquals(before, after, "el movimiento insertado antes del fallo no debió persistirse");
    }

    @Test
    void overpaymentIsRejectedBeforeAnyDatabaseWrite() {
        Person person = personService.createPerson("Test Sobrepago End To End", LocalDate.of(1990, 1, 1), null);
        savingService.registerSaving(person.id(), pesos("100000"), LocalDate.now(), PaymentMethod.CASH, null);
        Loan loan = loanService.createLoan(person.id(), pesos("100000"), 300, LocalDate.now(), PaymentMethod.CASH, null);

        assertThrows(IllegalArgumentException.class, () ->
                loanService.registerPayment(loan.id(), pesos("999999"), LocalDate.now(), PaymentMethod.CASH, null));

        Loan stillUnpaid = loanService.findById(loan.id()).orElseThrow();
        assertEquals(Money.ZERO, stillUnpaid.paidAmount(), "el intento rechazado no debió modificar el préstamo");
    }

    @Test
    void loanDisbursementIsRejectedWhenSavingsFundHasInsufficientBalance() {
        Person person = personService.createPerson("Test Saldo Insuficiente Prestamo", LocalDate.of(1990, 1, 1), null);
        savingService.registerSaving(person.id(), pesos("50000"), LocalDate.now(), PaymentMethod.CASH, null);

        int before = movementService.list(new MovementFilter(null, null, null, null, null, person.id(), null)).size();

        assertThrows(IllegalArgumentException.class, () ->
                loanService.createLoan(person.id(), pesos("999999999"), 300, LocalDate.now(), PaymentMethod.CASH, null));

        int after = movementService.list(new MovementFilter(null, null, null, null, null, person.id(), null)).size();
        assertEquals(before, after, "el desembolso rechazado no debió escribir ningún movimiento");
    }

    @Test
    void birthdayGiftIsRejectedWhenBirthdayFundHasInsufficientBalance() {
        Person person = personService.createPerson("Test Saldo Insuficiente Cumpleanos", LocalDate.of(1990, 1, 1), null);

        assertThrows(IllegalArgumentException.class, () ->
                birthdayGiftService.registerGift(person.id(), LocalDate.now(), pesos("999999999"), PaymentMethod.CASH, null));

        int movementsForPerson = movementService.list(new MovementFilter(null, null, null, null, null, person.id(), null)).size();
        assertEquals(0, movementsForPerson, "el regalo rechazado no debió escribir ningún movimiento");
    }

    @Test
    void editingASavingUpdatesTheSavingAndItsPairedMovementTogether() throws SQLException {
        Person person = personService.createPerson("Test Editar Ahorro", LocalDate.of(1990, 1, 1), null);
        Saving saving = savingService.registerSaving(person.id(), pesos("100000"), LocalDate.now(), PaymentMethod.CASH, "nota original");

        Saving updated = savingService.updateSaving(saving.id(), pesos("150000"), LocalDate.now(), PaymentMethod.TRANSFER, "nota corregida");

        assertEquals(pesos("150000"), updated.amount());
        assertEquals(PaymentMethod.TRANSFER, updated.paymentMethod());
        assertEquals("nota corregida", updated.notes());

        try (Connection connection = DatabaseManager.getConnection()) {
            Movement movement = movementRepository.findByReference(connection, "savings", saving.id()).orElseThrow();
            assertEquals(pesos("150000"), movement.amount());
            assertEquals(PaymentMethod.TRANSFER, movement.paymentMethod());
            assertEquals("nota corregida", movement.notes());
        }
    }

    @Test
    void reducingASavingBelowWhatWasAlreadyLentAgainstItIsRejected() {
        Person saver = personService.createPerson("Test Ahorrador Reduccion", LocalDate.of(1990, 1, 1), null);
        Saving saving = savingService.registerSaving(saver.id(), pesos("500000"), LocalDate.now(), PaymentMethod.CASH, null);

        // Borrow out the entire cash balance of the savings fund (as it stands right
        // after the contribution above), so the fund's cash bucket is left at exactly 0.
        Person borrower = personService.createPerson("Test Prestatario Reduccion", LocalDate.of(1990, 1, 1), null);
        FundBalances afterContribution = movementService.getBalances();
        loanService.createLoan(borrower.id(), afterContribution.savingsCash(), 300, LocalDate.now(), PaymentMethod.CASH, null);

        assertThrows(IllegalArgumentException.class, () ->
                        savingService.updateSaving(saving.id(), pesos("10000"), LocalDate.now(), PaymentMethod.CASH, null),
                "reducir el ahorro dejaría el fondo en efectivo en negativo dado el préstamo ya desembolsado");
    }

    @Test
    void settlementForAYearOnlyCountsThatYearsSavings() {
        Person person = personService.createPerson("Test Liquidacion Anual", LocalDate.of(1990, 1, 1), null);
        savingService.registerSaving(person.id(), pesos("1000000"), LocalDate.of(2025, 6, 1), PaymentMethod.CASH, null);
        savingService.registerSaving(person.id(), pesos("2000000"), LocalDate.of(2026, 6, 1), PaymentMethod.CASH, null);

        Settlement settlement2026 = settlementService.calculateForPerson(person.id(), 2026);

        assertEquals(pesos("2000000"), settlement2026.savingsTotal(), "no debe incluir el ahorro de 2025");
        assertEquals(pesos("60000"), settlement2026.interestAmount());
    }
}
