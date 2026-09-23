package com.playground.fondoahorro.infrastructure.repository;

import com.playground.fondoahorro.domain.entity.LoanInterestCharge;
import com.playground.fondoahorro.domain.outputport.LoanInterestChargeRepository;
import com.playground.fondoahorro.domain.enums.LoanInterestChargeStatus;
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

public class JdbcLoanInterestChargeRepository implements LoanInterestChargeRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcLoanInterestChargeRepository.class);

    @Override
    public LoanInterestCharge insert(Connection connection, LoanInterestCharge charge) throws SQLException {
        String sql = "INSERT INTO loan_interest_charges (loan_id, due_date, principal_balance_cents, interest_amount_cents, "
                + "paid_amount_cents, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, charge.loanId());
            ps.setString(2, charge.dueDate().toString());
            ps.setLong(3, charge.principalBalance().toCents());
            ps.setLong(4, charge.interestAmount().toCents());
            ps.setLong(5, charge.paidAmount().toCents());
            ps.setString(6, charge.status().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findByIdOnConnection(connection, keys.getLong(1));
            }
        }
    }

    @Override
    public void updatePayment(Connection connection, LoanInterestCharge charge) throws SQLException {
        String sql = "UPDATE loan_interest_charges SET paid_amount_cents = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, charge.paidAmount().toCents());
            ps.setString(2, charge.status().name());
            ps.setLong(3, charge.id());
            ps.executeUpdate();
        }
    }

    @Override
    public List<LoanInterestCharge> findUnpaidByLoan(Connection connection, long loanId) throws SQLException {
        String sql = "SELECT * FROM loan_interest_charges WHERE loan_id = ? AND status != 'PAID' ORDER BY due_date ASC, id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LoanInterestCharge> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapCharge(rs));
                }
                return results;
            }
        }
    }

    @Override
    public List<LoanInterestCharge> findByLoan(long loanId) {
        String sql = "SELECT * FROM loan_interest_charges WHERE loan_id = ? ORDER BY due_date DESC, id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LoanInterestCharge> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapCharge(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al consultar los cobros de interés del préstamo {}", loanId, e);
            throw new DataAccessException("No fue posible consultar los cobros de interés.", e);
        }
    }

    private LoanInterestCharge findByIdOnConnection(Connection connection, long id) throws SQLException {
        String sql = "SELECT * FROM loan_interest_charges WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapCharge(rs);
            }
        }
    }

    private LoanInterestCharge mapCharge(ResultSet rs) throws SQLException {
        return new LoanInterestCharge(
                rs.getLong("id"),
                rs.getLong("loan_id"),
                LocalDate.parse(rs.getString("due_date")),
                Money.ofCents(rs.getLong("principal_balance_cents")),
                Money.ofCents(rs.getLong("interest_amount_cents")),
                Money.ofCents(rs.getLong("paid_amount_cents")),
                LoanInterestChargeStatus.valueOf(rs.getString("status")),
                LocalDateTime.parse(rs.getString("created_at")));
    }
}
