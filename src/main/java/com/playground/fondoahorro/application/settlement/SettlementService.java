package com.playground.fondoahorro.application.settlement;

import com.playground.fondoahorro.application.settings.AppSettingsService;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.domain.person.PersonRepository;
import com.playground.fondoahorro.domain.savings.SavingRepository;
import com.playground.fondoahorro.domain.settlement.Settlement;
import com.playground.fondoahorro.domain.settlement.SettlementRepository;
import com.playground.fondoahorro.domain.settlement.SettlementRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calculates and stores annual settlements. A settlement never moves money
 * on its own — no movement or transfer is created when one is prepared (see
 * project decision: preparing/consulting is a calculation aid, not a payout).
 */
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final SavingRepository savingRepository;
    private final PersonRepository personRepository;
    private final AppSettingsService appSettingsService;

    public SettlementService(SettlementRepository settlementRepository, SavingRepository savingRepository,
                              PersonRepository personRepository, AppSettingsService appSettingsService) {
        this.settlementRepository = settlementRepository;
        this.savingRepository = savingRepository;
        this.personRepository = personRepository;
        this.appSettingsService = appSettingsService;
    }

    public Settlement calculateForPerson(long personId, int year) {
        Money savingsTotal = savingRepository.totalForPersonAndYear(personId, year);
        int rateBps = appSettingsService.getSettlementInterestRateBps();
        return Settlement.calculate(personId, year, savingsTotal, rateBps);
    }

    public List<SettlementRow> listForYear(int year) {
        List<Person> people = personRepository.findAll(null, false);
        List<SettlementRow> rows = new ArrayList<>();
        for (Person person : people) {
            Settlement calculated = calculateForPerson(person.id(), year);
            Optional<Settlement> prepared = settlementRepository.findByPersonAndYear(person.id(), year);
            rows.add(new SettlementRow(person, calculated, prepared.isPresent(),
                    prepared.map(Settlement::preparedAt).orElse(null)));
        }
        return rows;
    }

    public Settlement prepare(long personId, int year) {
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no existe."));
        Settlement calculated = calculateForPerson(personId, year);
        return settlementRepository.upsert(calculated);
    }

    public Optional<Settlement> findPrepared(long personId, int year) {
        return settlementRepository.findByPersonAndYear(personId, year);
    }

    public List<Settlement> historyForPerson(long personId) {
        return settlementRepository.findByPerson(personId);
    }
}
