package com.playground.fondoahorro.infrastructure.birthday;

import com.playground.fondoahorro.domain.birthday.BirthdayGift;
import com.playground.fondoahorro.domain.birthday.BirthdayGiftRepository;
import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.PaymentMethod;
import com.playground.fondoahorro.infrastructure.database.DataAccessException;
import com.playground.fondoahorro.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcBirthdayGiftRepository implements BirthdayGiftRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcBirthdayGiftRepository.class);

    @Override
    public BirthdayGift insert(Connection connection, BirthdayGift gift) throws SQLException {
        String sql = "INSERT INTO birthday_gifts (person_id, year, gift_date, amount_cents, payment_method, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, gift.personId());
            ps.setInt(2, gift.year());
            ps.setString(3, gift.giftDate().toString());
            ps.setLong(4, gift.amount().toCents());
            ps.setString(5, gift.paymentMethod().name());
            ps.setString(6, gift.notes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                return findByIdOnConnection(connection, id);
            }
        }
    }

    @Override
    public Optional<BirthdayGift> findByPersonAndYear(long personId, int year) {
        String sql = "SELECT * FROM birthday_gifts WHERE person_id = ? AND year = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, personId);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapGift(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Error al consultar regalo de cumpleaños de la persona {} en {}", personId, year, e);
            throw new DataAccessException("No fue posible consultar el regalo.", e);
        }
    }

    private BirthdayGift findByIdOnConnection(Connection connection, long id) throws SQLException {
        String sql = "SELECT * FROM birthday_gifts WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapGift(rs);
            }
        }
    }

    private BirthdayGift mapGift(ResultSet rs) throws SQLException {
        return new BirthdayGift(
                rs.getLong("id"),
                rs.getLong("person_id"),
                rs.getInt("year"),
                LocalDate.parse(rs.getString("gift_date")),
                Money.ofCents(rs.getLong("amount_cents")),
                PaymentMethod.valueOf(rs.getString("payment_method")),
                rs.getString("notes"),
                LocalDateTime.parse(rs.getString("created_at")));
    }
}
