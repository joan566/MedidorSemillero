package com.playground.fondoahorro.infrastructure.person;

import com.playground.fondoahorro.domain.money.Money;
import com.playground.fondoahorro.domain.person.Person;
import com.playground.fondoahorro.domain.person.PersonRepository;
import com.playground.fondoahorro.domain.person.PersonSummary;
import com.playground.fondoahorro.infrastructure.database.DatabaseManager;
import com.playground.fondoahorro.infrastructure.database.DataAccessException;
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

public class JdbcPersonRepository implements PersonRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcPersonRepository.class);

    @Override
    public Person insert(Person person) {
        String sql = "INSERT INTO persons (name, birth_date, phone, active) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, person.name());
            ps.setString(2, person.birthDate().toString());
            ps.setString(3, person.phone());
            ps.setInt(4, person.active() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                return findById(id).orElseThrow();
            }
        } catch (SQLException e) {
            log.error("Error al registrar persona", e);
            throw new DataAccessException("No fue posible registrar la persona.", e);
        }
    }

    @Override
    public void update(Person person) {
        String sql = "UPDATE persons SET name = ?, birth_date = ?, phone = ?, "
                + "updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now') WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, person.name());
            ps.setString(2, person.birthDate().toString());
            ps.setString(3, person.phone());
            ps.setLong(4, person.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al actualizar persona {}", person.id(), e);
            throw new DataAccessException("No fue posible actualizar la persona.", e);
        }
    }

    @Override
    public void setActive(long id, boolean active) {
        String sql = "UPDATE persons SET active = ?, updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now') WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al cambiar estado de persona {}", id, e);
            throw new DataAccessException("No fue posible actualizar el estado de la persona.", e);
        }
    }

    @Override
    public Optional<Person> findById(long id) {
        String sql = "SELECT * FROM persons WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPerson(rs));
            }
        } catch (SQLException e) {
            log.error("Error al consultar persona {}", id, e);
            throw new DataAccessException("No fue posible consultar la persona.", e);
        }
    }

    @Override
    public List<Person> findAll(String query, boolean includeInactive) {
        StringBuilder sql = new StringBuilder("SELECT * FROM persons WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, query, includeInactive, "");
        sql.append(" ORDER BY name");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Person> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapPerson(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al listar personas", e);
            throw new DataAccessException("No fue posible consultar las personas.", e);
        }
    }

    @Override
    public List<PersonSummary> findAllWithSummary(String query, boolean includeInactive) {
        StringBuilder sql = new StringBuilder(SUMMARY_SELECT + " WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, query, includeInactive, "p.");
        sql.append(" ORDER BY p.name");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<PersonSummary> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapSummary(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al listar personas con resumen", e);
            throw new DataAccessException("No fue posible consultar las personas.", e);
        }
    }

    @Override
    public PersonSummary getSummary(long id) {
        String sql = SUMMARY_SELECT + " WHERE p.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("La persona no existe.");
                }
                return mapSummary(rs);
            }
        } catch (SQLException e) {
            log.error("Error al consultar resumen de persona {}", id, e);
            throw new DataAccessException("No fue posible consultar el resumen de la persona.", e);
        }
    }

    private static final String SUMMARY_SELECT = "SELECT p.*, "
            + "COALESCE((SELECT SUM(s.amount_cents) FROM savings s WHERE s.person_id = p.id), 0) AS total_savings_cents, "
            + "COALESCE((SELECT SUM(l.outstanding_amount_cents) FROM loans l WHERE l.person_id = p.id AND l.status = 'ACTIVE'), 0) AS outstanding_debt_cents "
            + "FROM persons p";

    private static void appendFilters(StringBuilder sql, List<Object> params, String query, boolean includeInactive, String columnPrefix) {
        if (query != null && !query.isBlank()) {
            sql.append(" AND ").append(columnPrefix).append("name LIKE ? COLLATE NOCASE");
            params.add("%" + query.trim() + "%");
        }
        if (!includeInactive) {
            sql.append(" AND ").append(columnPrefix).append("active = 1");
        }
    }

    private static void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private Person mapPerson(ResultSet rs) throws SQLException {
        return new Person(
                rs.getLong("id"),
                rs.getString("name"),
                LocalDate.parse(rs.getString("birth_date")),
                rs.getString("phone"),
                rs.getInt("active") == 1,
                LocalDateTime.parse(rs.getString("created_at")),
                LocalDateTime.parse(rs.getString("updated_at")));
    }

    private PersonSummary mapSummary(ResultSet rs) throws SQLException {
        Person person = mapPerson(rs);
        Money totalSavings = Money.ofCents(rs.getLong("total_savings_cents"));
        Money outstandingDebt = Money.ofCents(rs.getLong("outstanding_debt_cents"));
        return new PersonSummary(person, totalSavings, outstandingDebt);
    }
}
