package com.playground.fondoahorro.application.dashboard;

import com.playground.fondoahorro.application.birthday.BirthdayGiftService;
import com.playground.fondoahorro.application.loan.LoanService;
import com.playground.fondoahorro.application.movement.MovementService;
import com.playground.fondoahorro.application.person.PersonService;
import com.playground.fondoahorro.domain.birthday.UpcomingBirthday;
import com.playground.fondoahorro.domain.dashboard.DashboardSnapshot;
import com.playground.fondoahorro.domain.loan.Loan;
import com.playground.fondoahorro.domain.loan.LoanStatus;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.FundBalances;
import com.playground.fondoahorro.domain.movement.MovementFilter;
import com.playground.fondoahorro.domain.movement.MovementListItem;

import java.util.List;

public class DashboardService {

    private static final int UPCOMING_BIRTHDAYS_LIMIT = 5;
    private static final int RECENT_ACTIVITY_LIMIT = 8;

    private final MovementService movementService;
    private final LoanService loanService;
    private final PersonService personService;
    private final BirthdayGiftService birthdayGiftService;

    public DashboardService(MovementService movementService, LoanService loanService, PersonService personService,
                             BirthdayGiftService birthdayGiftService) {
        this.movementService = movementService;
        this.loanService = loanService;
        this.personService = personService;
        this.birthdayGiftService = birthdayGiftService;
    }

    public DashboardSnapshot getSnapshot() {
        FundBalances balances = movementService.getBalances();

        List<Loan> activeLoans = loanService.list(LoanStatus.ACTIVE, null).stream()
                .map(item -> item.loan())
                .toList();
        Money loanedMoney = activeLoans.stream().map(Loan::principalAmount).reduce(Money.ZERO, Money::plus);
        Money outstandingDebt = activeLoans.stream().map(Loan::outstandingAmount).reduce(Money.ZERO, Money::plus);

        int activePersonCount = personService.list(null, false).size();

        List<UpcomingBirthday> upcomingBirthdays = birthdayGiftService.listUpcoming().stream()
                .limit(UPCOMING_BIRTHDAYS_LIMIT)
                .toList();

        List<MovementListItem> recentActivity = movementService.list(MovementFilter.empty()).stream()
                .limit(RECENT_ACTIVITY_LIMIT)
                .toList();

        return new DashboardSnapshot(balances, loanedMoney, outstandingDebt, activeLoans.size(),
                activePersonCount, upcomingBirthdays, recentActivity);
    }
}
