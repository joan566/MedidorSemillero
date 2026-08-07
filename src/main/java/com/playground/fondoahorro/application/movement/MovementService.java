package com.playground.fondoahorro.application.movement;

import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.Fund;
import com.playground.fondoahorro.domain.movement.FundBalances;
import com.playground.fondoahorro.domain.movement.Movement;
import com.playground.fondoahorro.domain.movement.MovementFilter;
import com.playground.fondoahorro.domain.movement.MovementKind;
import com.playground.fondoahorro.domain.movement.MovementListItem;
import com.playground.fondoahorro.domain.movement.MovementRepository;
import com.playground.fondoahorro.domain.movement.MovementType;
import com.playground.fondoahorro.domain.movement.MovementTypeRepository;
import com.playground.fondoahorro.domain.movement.PaymentMethod;
import com.playground.fondoahorro.domain.person.PersonRepository;
import com.playground.fondoahorro.infrastructure.database.TransactionRunner;

import java.time.LocalDate;
import java.util.List;

public class MovementService {

    private final MovementRepository repository;
    private final MovementTypeRepository movementTypeRepository;
    private final PersonRepository personRepository;

    public MovementService(MovementRepository repository, MovementTypeRepository movementTypeRepository,
                            PersonRepository personRepository) {
        this.repository = repository;
        this.movementTypeRepository = movementTypeRepository;
        this.personRepository = personRepository;
    }

    public List<MovementListItem> list(MovementFilter filter) {
        return repository.findAll(filter);
    }

    public FundBalances getBalances() {
        return repository.getBalances();
    }

    /**
     * Records a standalone movement against a custom (non-system) movement type — the
     * manual counterpart to the automatic flows in SavingService/BirthdayGiftService/LoanService,
     * which each hardcode their own system type and can't be reused for arbitrary categories.
     */
    public Movement registerMovement(long movementTypeId, Fund fund, PaymentMethod method, Money amount,
                                      LocalDate date, Long personId, String notes) {
        MovementType type = movementTypeRepository.findById(movementTypeId)
                .orElseThrow(() -> new IllegalArgumentException("El tipo de movimiento no existe."));
        if (!type.active()) {
            throw new IllegalArgumentException("\"" + type.name() + "\" está inactivo.");
        }
        if (type.isSystemType()) {
            throw new IllegalArgumentException(
                    "\"" + type.name() + "\" se registra automáticamente y no puede usarse aquí.");
        }
        if (personId != null) {
            personRepository.findById(personId)
                    .orElseThrow(() -> new IllegalArgumentException("La persona no existe."));
        }

        return TransactionRunner.run(connection -> {
            if (type.kind() == MovementKind.EXPENSE) {
                FundBalances balances = repository.getBalances(connection);
                if (!balances.hasSufficientBalance(fund, method, amount)) {
                    throw new IllegalArgumentException(
                            "El " + fundName(fund) + " no tiene saldo suficiente para registrar este movimiento.");
                }
            }
            Movement movement = Movement.create(type.id(), fund, method, type.kind(), amount, date, personId,
                    null, null, notes);
            return repository.insert(connection, movement);
        });
    }

    private static String fundName(Fund fund) {
        return switch (fund) {
            case SAVINGS -> "fondo de ahorro";
            case BIRTHDAY -> "fondo de cumpleaños";
        };
    }
}
