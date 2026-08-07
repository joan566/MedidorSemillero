package com.playground.fondoahorro.domain.dashboard;

import com.playground.fondoahorro.domain.birthday.UpcomingBirthday;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.FundBalances;
import com.playground.fondoahorro.domain.movement.MovementListItem;

import java.util.List;

/** Everything the dashboard shows, gathered from the modules that already own each number. */
public record DashboardSnapshot(
        FundBalances fundBalances,
        Money loanedMoney,
        Money outstandingDebt,
        int activeLoanCount,
        int activePersonCount,
        List<UpcomingBirthday> upcomingBirthdays,
        List<MovementListItem> recentActivity) {
}
