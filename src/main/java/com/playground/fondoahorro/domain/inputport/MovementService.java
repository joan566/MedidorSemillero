package com.playground.fondoahorro.domain.inputport;

import com.playground.fondoahorro.domain.entity.Movement;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.vo.FundBalances;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.vo.MovementFilter;
import com.playground.fondoahorro.domain.vo.MovementListItem;

import java.time.LocalDate;
import java.util.List;

public interface MovementService {

    List<MovementListItem> list(MovementFilter filter);

    FundBalances getBalances();

    /**
     * Records a standalone movement against a custom (non-system) movement type — the
     * manual counterpart to the automatic flows in SavingService/BirthdayGiftService/LoanService,
     * which each hardcode their own system type and can't be reused for arbitrary categories.
     */
    Movement registerMovement(long movementTypeId, Fund fund, PaymentMethod method, Money amount,
                               LocalDate date, Long personId, String notes);
}
