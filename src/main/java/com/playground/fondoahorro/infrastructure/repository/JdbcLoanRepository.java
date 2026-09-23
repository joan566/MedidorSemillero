package com.playground.fondoahorro.infrastructure.repository;

import com.playground.fondoahorro.domain.entity.Loan;
import com.playground.fondoahorro.domain.vo.LoanListItem;
import com.playground.fondoahorro.domain.outputport.LoanRepository;
import com.playground.fondoahorro.domain.enums.LoanStatus;
import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.infrastructure.exception.DataAccessException;
import com.playground.fondoahorro.infrastructure.config.DatabaseManager;
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

public class JdbcLoanRepository implements LoanRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcLoanRepository.class);

    @Override
    public Loan insert(Connection connection, Loan loan) throws SQLException {
        String sql = "INSERT INTO loans (person_id, principal_amount_cents, interest_rate_bps, principal_balance_cents, "
                + "interest_owed_cents, paid_amount_cents, loan_date, next_accrual_date, status, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, loan.personId());
            ps.setLong(2, loan.principalAmount().toCents());
            ps.setInt(3, loan.interestRateBps());
            ps.setLong(4, loan.principalBalance().toCents());
            ps.setLong(5, loan.interestOwed().toCents());
            ps.setLong(6, loan.paidAmount().toCents());
            ps.setString(7, loan.loanDate().toString());
            ps.setString(8, loan.nextAccrualDate().toString());
            ps.setString(9, loan.status().name());
            ps.setString(10, loan.notes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(connection, keys.getLong(1)).orElseThrow();
            }
        }
    }

    @Override
    public void updateBalance(Connection connection, Loan loan) throws SQLException {
        String sql = "UPDATE loans SET principal_balance_cents = ?, interest_owed_cents = ?, paid_amount_cents = ?, "
                + "next_accrual_date = ?, status = ?, updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now') WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loan.principalBalance().toCents());
            ps.setLong(2, loan.interestOwed().toCents());
            ps.setLong(3, loan.paidAmount().toCents());
            ps.setString(4, loan.nextAccrualDate().toString());
            ps.setString(5, loan.status().name());
            ps.setLong(6, loan.id());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Loan> findById(long id) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            log.error("Error al consultar préstamo {}", id, e);
            throw new DataAccessException("No fue posible consultar el préstamo.", e);
        }
    }

    @Override
    public Optional<Loan> findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT * FROM loans WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapLoan(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Loan> findByPerson(long personId) {
        String sql = "SELECT * FROM loans WHERE person_id = ? ORDER BY loan_date DESC, id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Loan> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapLoan(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al consultar préstamos de la persona {}", personId, e);
            throw new DataAccessException("No fue posible consultar los préstamos.", e);
        }
    }

    @Override
    public List<LoanListItem> findAll(LoanStatus statusFilter, Long personId) {
        StringBuilder sql = new StringBuilder(
                "SELECT l.*, p.name AS person_name FROM loans l JOIN persons p ON p.id = l.person_id WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (statusFilter != null) {
            sql.append(" AND l.status = ?");
            params.add(statusFilter.name());
        }
        if (personId != null) {
            sql.append(" AND l.person_id = ?");
            params.add(personId);
        }
        sql.append(" ORDER BY l.loan_date DESC, l.id DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<LoanListItem> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new LoanListItem(mapLoan(rs), rs.getString("person_name")));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al listar préstamos", e);
            throw new DataAccessException("No fue posible consultar los préstamos.", e);
        }
    }

    private Loan mapLoan(ResultSet rs) throws SQLException {
        return new Loan(
                rs.getLong("id"),
                rs.getLong("person_id"),
                Money.ofCents(rs.getLong("principal_amount_cents")),
                rs.getInt("interest_rate_bps"),
                Money.ofCents(rs.getLong("principal_balance_cents")),
                Money.ofCents(rs.getLong("interest_owed_cents")),
                Money.ofCents(rs.getLong("paid_amount_cents")),
                LocalDate.parse(rs.getString("loan_date")),
                LocalDate.parse(rs.getString("next_accrual_date")),
                LoanStatus.valueOf(rs.getString("status")),
                rs.getString("notes"),
                LocalDateTime.parse(rs.getString("created_at")),
                LocalDateTime.parse(rs.getString("updated_at")));
    }
}
