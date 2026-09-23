package com.playground.fondoahorro.domain.vo;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.entity.Person;

import java.time.LocalDate;

/**
 * A person's next birthday occurrence, with the turning age and whether that
 * year's gift has already been given. daysUntil may be negative — a birthday
 * within the recent-past grace window still counts as "upcoming" so the
 * administrator doesn't lose track of an unfinished gift. givenAmount is
 * null unless a gift was actually given for that occurrence's year.
 */
public record UpcomingBirthday(Person person, LocalDate occurrenceDate, int turningAge, long daysUntil,
                                boolean giftGiven, Money givenAmount) {
}
