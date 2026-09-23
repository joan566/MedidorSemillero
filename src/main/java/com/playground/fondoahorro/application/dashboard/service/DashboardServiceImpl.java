package com.playground.fondoahorro.application.dashboard.service;

import com.playground.fondoahorro.domain.inputport.BirthdayGiftService;
import com.playground.fondoahorro.domain.inputport.LoanService;
import com.playground.fondoahorro.domain.inputport.MovementService;
import com.playground.fondoahorro.domain.inputport.PersonService;
import com.playground.fondoahorro.domain.inputport.DashboardService;
import com.playground.fondoahorro.domain.vo.UpcomingBirthday;
import com.playground.fondoahorro.domain.vo.DashboardSnapshot;
import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.enums.LoanStatus;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.vo.FundBalances;
import com.playground.fondoahorro.domain.vo.MovementFilter;
import com.playground.fondoahorro.domain.vo.MovementListItem;

import java.util.List;

public class DashboardServiceImpl implements DashboardService {

    private static final int UPCOMING_BIRTHDAYS_LIMIT = 5;
    private static final int RECENT_ACTIVITY_LIMIT = 8;

    private final MovementService movementService;
    private final LoanService loanService;
    private final PersonService personService;
    private final BirthdayGiftService birthdayGiftService;

    public DashboardServiceImpl(MovementService movementService, LoanService loanService, PersonService personService,
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
        Money outstandingDebt = activeLoans.stream().map(Loan::totalOwed).reduce(Money.ZERO, Money::plus);

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
