package com.playground.fondoahorro.domain.vo;

import com.playground.fondoahorro.domain.vo.UpcomingBirthday;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.vo.FundBalances;
import com.playground.fondoahorro.domain.vo.MovementListItem;

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
