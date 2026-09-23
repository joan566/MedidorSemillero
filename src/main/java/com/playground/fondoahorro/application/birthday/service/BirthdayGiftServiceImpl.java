package com.playground.fondoahorro.application.birthday.service;

import com.playground.fondoahorro.domain.inputport.AppSettingsService;
import com.playground.fondoahorro.domain.inputport.BirthdayGiftService;
import com.playground.fondoahorro.domain.entity.BirthdayGift;
import com.playground.fondoahorro.domain.outputport.BirthdayGiftRepository;
import com.playground.fondoahorro.domain.vo.UpcomingBirthday;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.vo.FundBalances;
import com.playground.fondoahorro.domain.entity.Movement;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.outputport.MovementRepository;
import com.playground.fondoahorro.domain.entity.MovementType;
import com.playground.fondoahorro.domain.outputport.MovementTypeRepository;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.entity.Person;
import com.playground.fondoahorro.domain.outputport.PersonRepository;
import com.playground.fondoahorro.infrastructure.transaction.TransactionRunner;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BirthdayGiftServiceImpl implements BirthdayGiftService {

    private static final String BIRTHDAY_GIFT_CODE = "BIRTHDAY_GIFT";

    /**
     * A birthday that passed within this many days still counts as
     * "upcoming" (instead of jumping straight to next year) so a gift that
     * hasn't been given yet isn't immediately lost from view.
     */
    private static final int GRACE_DAYS = 14;

    private final BirthdayGiftRepository giftRepository;
    private final MovementRepository movementRepository;
    private final MovementTypeRepository movementTypeRepository;
    private final PersonRepository personRepository;
    private final AppSettingsService appSettingsService;

    public BirthdayGiftServiceImpl(BirthdayGiftRepository giftRepository, MovementRepository movementRepository,
                                MovementTypeRepository movementTypeRepository, PersonRepository personRepository,
                                AppSettingsService appSettingsService) {
        this.giftRepository = giftRepository;
        this.movementRepository = movementRepository;
        this.movementTypeRepository = movementTypeRepository;
        this.personRepository = personRepository;
        this.appSettingsService = appSettingsService;
    }

    public Money defaultGiftAmount() {
        return appSettingsService.getBirthdayGiftDefaultAmount();
    }

    public List<UpcomingBirthday> listUpcoming() {
        LocalDate today = LocalDate.now();
        List<Person> activePeople = personRepository.findAll(null, false);

        List<UpcomingBirthday> result = new ArrayList<>();
        for (Person person : activePeople) {
            LocalDate occurrence = nextOccurrence(person.birthDate(), today);
            long daysUntil = ChronoUnit.DAYS.between(today, occurrence);
            int turningAge = occurrence.getYear() - person.birthDate().getYear();
            var existingGift = giftRepository.findByPersonAndYear(person.id(), occurrence.getYear());
            Money givenAmount = existingGift.map(BirthdayGift::amount).orElse(null);
            result.add(new UpcomingBirthday(person, occurrence, turningAge, daysUntil, existingGift.isPresent(), givenAmount));
        }
        result.sort(Comparator.comparingLong(UpcomingBirthday::daysUntil));
        return result;
    }

    private LocalDate nextOccurrence(LocalDate birthDate, LocalDate today) {
        LocalDate candidate = MonthDay.from(birthDate).atYear(today.getYear());
        LocalDate graceFloor = today.minusDays(GRACE_DAYS);
        if (candidate.isBefore(graceFloor)) {
            candidate = MonthDay.from(birthDate).atYear(today.getYear() + 1);
        }
        return candidate;
    }

    public BirthdayGift registerGift(long personId, LocalDate giftDate, Money amount, PaymentMethod method, String notes) {
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no existe."));

        int year = giftDate.getYear();
        if (giftRepository.findByPersonAndYear(personId, year).isPresent()) {
            throw new IllegalArgumentException("Ya se registró un regalo para esta persona en " + year + ".");
        }

        MovementType type = movementTypeRepository.findByCode(BIRTHDAY_GIFT_CODE)
                .orElseThrow(() -> new IllegalStateException("No se encontró el tipo de movimiento de regalo."));

        BirthdayGift gift = BirthdayGift.create(personId, giftDate, amount, method, notes);

        return TransactionRunner.run(connection -> {
            FundBalances balances = movementRepository.getBalances(connection);
            if (!balances.hasSufficientBalance(Fund.BIRTHDAY, method, amount)) {
                throw new IllegalArgumentException(
                        "El fondo de cumpleaños no tiene saldo suficiente para registrar este regalo.");
            }
            BirthdayGift inserted = giftRepository.insert(connection, gift);
            Movement movement = Movement.create(type.id(), Fund.BIRTHDAY, method, MovementKind.EXPENSE,
                    amount, giftDate, personId, "birthday_gifts", inserted.id(), notes);
            movementRepository.insert(connection, movement);
            return inserted;
        });
    }
}
