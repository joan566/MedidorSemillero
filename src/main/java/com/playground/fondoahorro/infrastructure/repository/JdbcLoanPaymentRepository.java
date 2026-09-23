package com.playground.fondoahorro.infrastructure.repository;

import com.playground.fondoahorro.domain.entity.LoanPayment;
import com.playground.fondoahorro.domain.outputport.LoanPaymentRepository;
import com.playground.fondoahorro.domain.vo.Money;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcLoanPaymentRepository implements LoanPaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcLoanPaymentRepository.class);

    @Override
    public LoanPayment insert(Connection connection, LoanPayment payment) throws SQLException {
        String sql = "INSERT INTO loan_payments (loan_id, amount_cents, interest_portion_cents, principal_portion_cents, "
                + "payment_date, payment_method, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, payment.loanId());
            ps.setLong(2, payment.amount().toCents());
            ps.setLong(3, payment.interestPortion().toCents());
            ps.setLong(4, payment.principalPortion().toCents());
            ps.setString(5, payment.paymentDate().toString());
            ps.setString(6, payment.paymentMethod().name());
            ps.setString(7, payment.notes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findByIdOnConnection(connection, keys.getLong(1));
            }
        }
    }

    @Override
    public List<LoanPayment> findByLoan(long loanId) {
        String sql = "SELECT * FROM loan_payments WHERE loan_id = ? ORDER BY payment_date DESC, id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LoanPayment> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapPayment(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al consultar pagos del préstamo {}", loanId, e);
            throw new DataAccessException("No fue posible consultar los pagos.", e);
        }
    }

    private LoanPayment findByIdOnConnection(Connection connection, long id) throws SQLException {
        String sql = "SELECT * FROM loan_payments WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapPayment(rs);
            }
        }
    }

    private LoanPayment mapPayment(ResultSet rs) throws SQLException {
        return new LoanPayment(
                rs.getLong("id"),
                rs.getLong("loan_id"),
                Money.ofCents(rs.getLong("amount_cents")),
                Money.ofCents(rs.getLong("interest_portion_cents")),
                Money.ofCents(rs.getLong("principal_portion_cents")),
                LocalDate.parse(rs.getString("payment_date")),
                PaymentMethod.valueOf(rs.getString("payment_method")),
                rs.getString("notes"),
                LocalDateTime.parse(rs.getString("created_at")));
    }
}
