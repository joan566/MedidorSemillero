package com.playground.fondoahorro.infrastructure.savings;

import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.movement.PaymentMethod;
import com.playground.fondoahorro.domain.savings.Saving;
import com.playground.fondoahorro.domain.savings.SavingListItem;
import com.playground.fondoahorro.domain.savings.SavingRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSavingRepository implements SavingRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcSavingRepository.class);

    @Override
    public Saving insert(Connection connection, Saving saving) throws SQLException {
        String sql = "INSERT INTO savings (person_id, amount_cents, saving_date, payment_method, notes) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, saving.personId());
            ps.setLong(2, saving.amount().toCents());
            ps.setString(3, saving.date().toString());
            ps.setString(4, saving.paymentMethod().name());
            ps.setString(5, saving.notes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                return findByIdOnConnection(connection, id);
            }
        }
    }

    @Override
    public Optional<Saving> findById(long id) {
        String sql = "SELECT * FROM savings WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapSaving(rs));
            }
        } catch (SQLException e) {
            log.error("Error al consultar el ahorro {}", id, e);
            throw new DataAccessException("No fue posible consultar el ahorro.", e);
        }
    }

    @Override
    public Saving update(Connection connection, Saving saving) throws SQLException {
        String sql = "UPDATE savings SET amount_cents = ?, saving_date = ?, payment_method = ?, notes = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, saving.amount().toCents());
            ps.setString(2, saving.date().toString());
            ps.setString(3, saving.paymentMethod().name());
            ps.setString(4, saving.notes());
            ps.setLong(5, saving.id());
            ps.executeUpdate();
        }
        return findByIdOnConnection(connection, saving.id());
    }

    @Override
    public List<Saving> findByPerson(long personId) {
        String sql = "SELECT * FROM savings WHERE person_id = ? ORDER BY saving_date DESC, id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Saving> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapSaving(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al consultar ahorros de la persona {}", personId, e);
            throw new DataAccessException("No fue posible consultar los ahorros.", e);
        }
    }

    @Override
    public List<SavingListItem> findAll(Long personId) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, p.name AS person_name FROM savings s JOIN persons p ON p.id = s.person_id WHERE 1 = 1");
        if (personId != null) {
            sql.append(" AND s.person_id = ?");
        }
        sql.append(" ORDER BY s.saving_date DESC, s.id DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (personId != null) {
                ps.setLong(1, personId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<SavingListItem> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new SavingListItem(mapSaving(rs), rs.getString("person_name")));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al listar ahorros", e);
            throw new DataAccessException("No fue posible consultar los ahorros.", e);
        }
    }

    @Override
    public Money totalForPersonAndYear(long personId, int year) {
        String sql = "SELECT COALESCE(SUM(amount_cents), 0) AS total FROM savings "
                + "WHERE person_id = ? AND strftime('%Y', saving_date) = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, personId);
            ps.setString(2, String.valueOf(year));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return Money.ofCents(rs.getLong("total"));
            }
        } catch (SQLException e) {
            log.error("Error al calcular el total ahorrado por la persona {} en {}", personId, year, e);
            throw new DataAccessException("No fue posible calcular el total ahorrado.", e);
        }
    }

    private Saving findByIdOnConnection(Connection connection, long id) throws SQLException {
        String sql = "SELECT * FROM savings WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapSaving(rs);
            }
        }
    }

    private Saving mapSaving(ResultSet rs) throws SQLException {
        return new Saving(
                rs.getLong("id"),
                rs.getLong("person_id"),
                Money.ofCents(rs.getLong("amount_cents")),
                LocalDate.parse(rs.getString("saving_date")),
                PaymentMethod.valueOf(rs.getString("payment_method")),
                rs.getString("notes"),
                LocalDateTime.parse(rs.getString("created_at")));
    }
}
