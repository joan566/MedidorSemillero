package com.playground.fondoahorro.domain.outputport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import com.playground.fondoahorro.domain.entity.BirthdayGift;

public interface BirthdayGiftRepository {

    BirthdayGift insert(Connection connection, BirthdayGift gift) throws SQLException;

    Optional<BirthdayGift> findByPersonAndYear(long personId, int year);
}
