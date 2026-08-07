package com.playground.fondoahorro.domain.birthday;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public interface BirthdayGiftRepository {

    BirthdayGift insert(Connection connection, BirthdayGift gift) throws SQLException;

    Optional<BirthdayGift> findByPersonAndYear(long personId, int year);
}
