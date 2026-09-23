package com.playground.fondoahorro.application.savings.service;

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
import com.playground.fondoahorro.domain.entity.Saving;
import com.playground.fondoahorro.domain.inputport.SavingService;
import com.playground.fondoahorro.domain.vo.SavingListItem;
import com.playground.fondoahorro.domain.outputport.SavingRepository;
import com.playground.fondoahorro.infrastructure.transaction.TransactionRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SavingServiceImpl implements SavingService {

    private static final String SAVINGS_CONTRIBUTION_CODE = "SAVINGS_CONTRIBUTION";

    private final SavingRepository savingRepository;
    private final MovementRepository movementRepository;
    private final MovementTypeRepository movementTypeRepository;
    private final PersonRepository personRepository;

    public SavingServiceImpl(SavingRepository savingRepository, MovementRepository movementRepository,
                          MovementTypeRepository movementTypeRepository, PersonRepository personRepository) {
        this.savingRepository = savingRepository;
        this.movementRepository = movementRepository;
        this.movementTypeRepository = movementTypeRepository;
        this.personRepository = personRepository;
    }

    public Saving registerSaving(long personId, Money amount, LocalDate date, PaymentMethod method, String notes) {
        return TransactionRunner.run(connection -> registerSaving(connection, personId, amount, date, method, notes));
    }

    /** Same as registerSaving(...), but runs on a caller-supplied connection — used by the Excel importer to replay historical savings inside one shared transaction. */
    public Saving registerSaving(Connection connection, long personId, Money amount, LocalDate date, PaymentMethod method,
                                  String notes) throws SQLException {
        personRepository.findById(connection, personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no existe."));
        MovementType type = movementTypeRepository.findByCode(SAVINGS_CONTRIBUTION_CODE)
                .orElseThrow(() -> new IllegalStateException("No se encontró el tipo de movimiento de ahorro."));

        Saving saving = Saving.create(personId, amount, date, method, notes);
        Saving inserted = savingRepository.insert(connection, saving);
        Movement movement = Movement.create(type.id(), Fund.SAVINGS, method, MovementKind.INCOME,
                amount, date, personId, "savings", inserted.id(), notes);
        movementRepository.insert(connection, movement);
        return inserted;
    }

    public Saving updateSaving(long savingId, Money amount, LocalDate date, PaymentMethod method, String notes) {
        Saving existing = savingRepository.findById(savingId)
                .orElseThrow(() -> new IllegalArgumentException("El ahorro no existe."));
        Saving updated = existing.withUpdatedDetails(amount, date, method, notes);

        return TransactionRunner.run(connection -> {
            Movement existingMovement = movementRepository.findByReference(connection, "savings", savingId)
                    .orElseThrow(() -> new IllegalStateException("No se encontró el movimiento asociado a este ahorro."));

            FundBalances balances = movementRepository.getBalances(connection);
            Money bucketAfterRemoval = balances.balanceFor(Fund.SAVINGS, existingMovement.paymentMethod())
                    .minus(existingMovement.amount());
            if (existingMovement.paymentMethod() == method) {
                bucketAfterRemoval = bucketAfterRemoval.plus(amount);
            }
            if (bucketAfterRemoval.compareTo(Money.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "No es posible actualizar este ahorro: ya hay préstamos u otros movimientos "
                                + "registrados que dependen de este saldo.");
            }

            Saving savedSaving = savingRepository.update(connection, updated);
            Movement updatedMovement = existingMovement.withUpdatedDetails(method, amount, date, notes);
            movementRepository.update(connection, updatedMovement);
            return savedSaving;
        });
    }

    public List<Saving> historyForPerson(long personId) {
        return savingRepository.findByPerson(personId);
    }

    public List<SavingListItem> list(Long personId) {
        return savingRepository.findAll(personId);
    }
}
