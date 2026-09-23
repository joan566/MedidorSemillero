package com.playground.fondoahorro.domain.inputport;

import com.playground.fondoahorro.domain.entity.BirthdayGift;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.vo.UpcomingBirthday;

import java.time.LocalDate;
import java.util.List;

public interface BirthdayGiftService {

    Money defaultGiftAmount();

    List<UpcomingBirthday> listUpcoming();

    BirthdayGift registerGift(long personId, LocalDate giftDate, Money amount, PaymentMethod method, String notes);
}
