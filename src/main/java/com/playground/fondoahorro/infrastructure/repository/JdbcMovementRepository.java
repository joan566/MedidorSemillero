package com.playground.fondoahorro.infrastructure.repository;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.enums.Fund;
import com.playground.fondoahorro.domain.vo.FundBalances;
import com.playground.fondoahorro.domain.entity.Movement;
import com.playground.fondoahorro.domain.vo.MovementFilter;
import com.playground.fondoahorro.domain.enums.MovementKind;
import com.playground.fondoahorro.domain.vo.MovementListItem;
import com.playground.fondoahorro.domain.outputport.MovementRepository;
import com.playground.fondoahorro.domain.enums.PaymentMethod;
import com.playground.fondoahorro.infrastructure.exception.DataAccessException;
import com.playground.fondoahorro.infrastructure.config.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcMovementRepository implements MovementRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcMovementRepository.class);

    @Override
    public Movement insert(Connection connection, Movement movement) throws SQLException {
        String sql = "INSERT INTO movements (movement_type_id, fund, payment_method, kind, amount_cents, "
                + "movement_date, person_id, reference_table, reference_id, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, movement.movementTypeId());
            ps.setString(2, movement.fund().name());
            ps.setString(3, movement.paymentMethod().name());
            ps.setString(4, movement.kind().name());
            ps.setLong(5, movement.amount().toCents());
            ps.setString(6, movement.date().toString());
            setNullableLong(ps, 7, movement.personId());
            ps.setString(8, movement.referenceTable());
            setNullableLong(ps, 9, movement.referenceId());
            ps.setString(10, movement.notes());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                return findByIdOnConnection(connection, id);
            }
        }
    }

    @Override
    public List<MovementListItem> findAll(MovementFilter filter) {
        StringBuilder sql = new StringBuilder(
                "SELECT m.*, mt.name AS type_name, p.name AS person_name "
                        + "FROM movements m "
                        + "JOIN movement_types mt ON mt.id = m.movement_type_id "
                        + "LEFT JOIN persons p ON p.id = m.person_id "
                        + "WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (filter.fromDate() != null) {
            sql.append(" AND m.movement_date >= ?");
            params.add(filter.fromDate().toString());
        }
        if (filter.toDate() != null) {
            sql.append(" AND m.movement_date <= ?");
            params.add(filter.toDate().toString());
        }
        if (filter.kind() != null) {
            sql.append(" AND m.kind = ?");
            params.add(filter.kind().name());
        }
        if (filter.fund() != null) {
            sql.append(" AND m.fund = ?");
            params.add(filter.fund().name());
        }
        if (filter.paymentMethod() != null) {
            sql.append(" AND m.payment_method = ?");
            params.add(filter.paymentMethod().name());
        }
        if (filter.personId() != null) {
            sql.append(" AND m.person_id = ?");
            params.add(filter.personId());
        }
        if (filter.movementTypeId() != null) {
            sql.append(" AND m.movement_type_id = ?");
            params.add(filter.movementTypeId());
        }
        sql.append(" ORDER BY m.movement_date DESC, m.id DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<MovementListItem> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapListItem(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al listar movimientos", e);
            throw new DataAccessException("No fue posible consultar los movimientos.", e);
        }
    }

    @Override
    public FundBalances getBalances() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getBalances(conn);
        } catch (SQLException e) {
            log.error("Error al calcular saldos de los fondos", e);
            throw new DataAccessException("No fue posible calcular los saldos.", e);
        }
    }

    @Override
    public FundBalances getBalances(Connection connection) throws SQLException {
        String sql = "SELECT fund, payment_method, "
                + "SUM(CASE WHEN kind = 'INCOME' THEN amount_cents ELSE -amount_cents END) AS balance_cents "
                + "FROM movements GROUP BY fund, payment_method";

        long savingsCash = 0;
        long savingsTransfer = 0;
        long birthdayCash = 0;
        long birthdayTransfer = 0;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Fund fund = Fund.valueOf(rs.getString("fund"));
                PaymentMethod method = PaymentMethod.valueOf(rs.getString("payment_method"));
                long cents = rs.getLong("balance_cents");
                if (fund == Fund.SAVINGS && method == PaymentMethod.CASH) {
                    savingsCash = cents;
                } else if (fund == Fund.SAVINGS && method == PaymentMethod.TRANSFER) {
                    savingsTransfer = cents;
                } else if (fund == Fund.BIRTHDAY && method == PaymentMethod.CASH) {
                    birthdayCash = cents;
                } else if (fund == Fund.BIRTHDAY && method == PaymentMethod.TRANSFER) {
                    birthdayTransfer = cents;
                }
            }
        }

        return new FundBalances(
                Money.ofCents(savingsCash),
                Money.ofCents(savingsTransfer),
                Money.ofCents(birthdayCash),
                Money.ofCents(birthdayTransfer));
    }

    @Override
    public Optional<Movement> findByReference(Connection connection, String referenceTable, long referenceId) throws SQLException {
        String sql = "SELECT * FROM movements WHERE reference_table = ? AND reference_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, referenceTable);
            ps.setLong(2, referenceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapMovement(rs));
            }
        }
    }

    @Override
    public void update(Connection connection, Movement movement) throws SQLException {
        String sql = "UPDATE movements SET payment_method = ?, amount_cents = ?, movement_date = ?, notes = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, movement.paymentMethod().name());
            ps.setLong(2, movement.amount().toCents());
            ps.setString(3, movement.date().toString());
            ps.setString(4, movement.notes());
            ps.setLong(5, movement.id());
            ps.executeUpdate();
        }
    }

    private Movement findByIdOnConnection(Connection connection, long id) throws SQLException {
        String sql = "SELECT * FROM movements WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapMovement(rs);
            }
        }
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private Movement mapMovement(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long movementTypeId = rs.getLong("movement_type_id");
        Fund fund = Fund.valueOf(rs.getString("fund"));
        PaymentMethod paymentMethod = PaymentMethod.valueOf(rs.getString("payment_method"));
        MovementKind kind = MovementKind.valueOf(rs.getString("kind"));
        Money amount = Money.ofCents(rs.getLong("amount_cents"));
        LocalDate date = LocalDate.parse(rs.getString("movement_date"));

        // wasNull() only reflects the most recent get*, so nullable columns
        // must be read and checked immediately, not after later get* calls.
        long personIdRaw = rs.getLong("person_id");
        Long personId = rs.wasNull() ? null : personIdRaw;

        String referenceTable = rs.getString("reference_table");

        long referenceIdRaw = rs.getLong("reference_id");
        Long referenceId = rs.wasNull() ? null : referenceIdRaw;

        String notes = rs.getString("notes");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));

        return new Movement(id, movementTypeId, fund, paymentMethod, kind, amount, date, personId,
                referenceTable, referenceId, notes, createdAt);
    }

    private MovementListItem mapListItem(ResultSet rs) throws SQLException {
        Movement movement = mapMovement(rs);
        String typeName = rs.getString("type_name");
        String personName = rs.getString("person_name");
        return new MovementListItem(movement, typeName, personName);
    }
}
