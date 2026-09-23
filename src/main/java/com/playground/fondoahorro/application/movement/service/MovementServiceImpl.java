package com.playground.fondoahorro.application.movement.service;

import com.playground.fondoahorro.domain.inputport.MovementService;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.vo.FundBalances;
import com.playground.fondoahorro.domain.entity.Movement;
import com.playground.fondoahorro.domain.vo.MovementFilter;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.vo.MovementListItem;
import com.playground.fondoahorro.domain.outputport.MovementRepository;
import com.playground.fondoahorro.domain.entity.MovementType;
import com.playground.fondoahorro.domain.outputport.MovementTypeRepository;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.outputport.PersonRepository;
import com.playground.fondoahorro.infrastructure.transaction.TransactionRunner;

import java.time.LocalDate;
import java.util.List;

public class MovementServiceImpl implements MovementService {

    private final MovementRepository repository;
    private final MovementTypeRepository movementTypeRepository;
    private final PersonRepository personRepository;

    public MovementServiceImpl(MovementRepository repository, MovementTypeRepository movementTypeRepository,
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
