package com.playground.fondoahorro.domain.inputport;

import com.playground.fondoahorro.domain.vo.Money;

public interface AppSettingsService {

    int getLoanInterestRateBps();

    void setLoanInterestRateBps(int bps);

    Money getBirthdayGiftDefaultAmount();

    void setBirthdayGiftDefaultAmount(Money amount);

    int getSettlementInterestRateBps();

    void setSettlementInterestRateBps(int bps);
}
