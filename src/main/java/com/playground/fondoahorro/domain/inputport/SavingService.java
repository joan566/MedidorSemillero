package com.playground.fondoahorro.domain.inputport;

import com.playground.fondoahorro.domain.entity.Saving;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.vo.SavingListItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface SavingService {

    Saving registerSaving(long personId, Money amount, LocalDate date, PaymentMethod method, String notes);

    /** Same as registerSaving(...), but runs on a caller-supplied connection — used by the Excel importer to replay historical savings inside one shared transaction. */
    Saving registerSaving(Connection connection, long personId, Money amount, LocalDate date, PaymentMethod method,
                           String notes) throws SQLException;

    Saving updateSaving(long savingId, Money amount, LocalDate date, PaymentMethod method, String notes);

    List<Saving> historyForPerson(long personId);

    List<SavingListItem> list(Long personId);
}
