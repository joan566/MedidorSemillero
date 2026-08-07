package com.playground.fondoahorro.application.savings;

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
import com.playground.fondoahorro.domain.savings.Saving;
import com.playground.fondoahorro.domain.savings.SavingListItem;
import com.playground.fondoahorro.domain.savings.SavingRepository;
import com.playground.fondoahorro.infrastructure.database.TransactionRunner;

import java.time.LocalDate;
import java.util.List;

public class SavingService {

    private static final String SAVINGS_CONTRIBUTION_CODE = "SAVINGS_CONTRIBUTION";

    private final SavingRepository savingRepository;
    private final MovementRepository movementRepository;
    private final MovementTypeRepository movementTypeRepository;
    private final PersonRepository personRepository;

    public SavingService(SavingRepository savingRepository, MovementRepository movementRepository,
                          MovementTypeRepository movementTypeRepository, PersonRepository personRepository) {
        this.savingRepository = savingRepository;
        this.movementRepository = movementRepository;
        this.movementTypeRepository = movementTypeRepository;
        this.personRepository = personRepository;
    }

    public Saving registerSaving(long personId, Money amount, LocalDate date, PaymentMethod method, String notes) {
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no existe."));
        MovementType type = movementTypeRepository.findByCode(SAVINGS_CONTRIBUTION_CODE)
                .orElseThrow(() -> new IllegalStateException("No se encontró el tipo de movimiento de ahorro."));

        Saving saving = Saving.create(personId, amount, date, method, notes);

        return TransactionRunner.run(connection -> {
            Saving inserted = savingRepository.insert(connection, saving);
            Movement movement = Movement.create(type.id(), Fund.SAVINGS, method, MovementKind.INCOME,
                    amount, date, personId, "savings", inserted.id(), notes);
            movementRepository.insert(connection, movement);
            return inserted;
        });
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
