package com.playground.fondoahorro.application.settings.service;

import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.outputport.AppSettingsRepository;

public class AppSettingsServiceImpl implements AppSettingsService {

    private static final String LOAN_INTEREST_RATE_BPS = "loan.interest_rate_bps";
    private static final int DEFAULT_LOAN_INTEREST_RATE_BPS = 300;

    private static final String BIRTHDAY_GIFT_DEFAULT_AMOUNT_CENTS = "birthday.gift_default_amount_cents";
    private static final long DEFAULT_BIRTHDAY_GIFT_AMOUNT_CENTS = 20_000_000L;

    private static final String SETTLEMENT_INTEREST_RATE_BPS = "settlement.interest_rate_bps";
    private static final int DEFAULT_SETTLEMENT_INTEREST_RATE_BPS = 300;

    private final AppSettingsRepository repository;

    public AppSettingsServiceImpl(AppSettingsRepository repository) {
        this.repository = repository;
    }

    public int getLoanInterestRateBps() {
        return repository.get(LOAN_INTEREST_RATE_BPS)
                .map(Integer::parseInt)
                .orElse(DEFAULT_LOAN_INTEREST_RATE_BPS);
    }

    public void setLoanInterestRateBps(int bps) {
        if (bps < 0) {
            throw new IllegalArgumentException("La tasa de interés no puede ser negativa.");
        }
        repository.set(LOAN_INTEREST_RATE_BPS, String.valueOf(bps));
    }

    public Money getBirthdayGiftDefaultAmount() {
        long cents = repository.get(BIRTHDAY_GIFT_DEFAULT_AMOUNT_CENTS)
                .map(Long::parseLong)
                .orElse(DEFAULT_BIRTHDAY_GIFT_AMOUNT_CENTS);
        return Money.ofCents(cents);
    }

    public void setBirthdayGiftDefaultAmount(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Ingresa un valor mayor a $0.");
        }
        repository.set(BIRTHDAY_GIFT_DEFAULT_AMOUNT_CENTS, String.valueOf(amount.toCents()));
    }

    public int getSettlementInterestRateBps() {
        return repository.get(SETTLEMENT_INTEREST_RATE_BPS)
                .map(Integer::parseInt)
                .orElse(DEFAULT_SETTLEMENT_INTEREST_RATE_BPS);
    }

    public void setSettlementInterestRateBps(int bps) {
        if (bps < 0) {
            throw new IllegalArgumentException("La tasa de interés no puede ser negativa.");
        }
        repository.set(SETTLEMENT_INTEREST_RATE_BPS, String.valueOf(bps));
    }
}
