package com.playground.fondoahorro.infrastructure.repository;

import com.playground.fondoahorro.domain.vo.Money;
import com.playground.fondoahorro.domain.entity.Settlement;
import com.playground.fondoahorro.domain.outputport.SettlementRepository;
import com.playground.fondoahorro.infrastructure.exception.DataAccessException;
import com.playground.fondoahorro.infrastructure.config.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSettlementRepository implements SettlementRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcSettlementRepository.class);

    @Override
    public Settlement upsert(Settlement settlement) {
        String sql = "INSERT INTO settlements (person_id, year, savings_total_cents, interest_rate_bps, "
                + "interest_amount_cents, total_amount_cents) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(person_id, year) DO UPDATE SET "
                + "savings_total_cents = excluded.savings_total_cents, "
                + "interest_rate_bps = excluded.interest_rate_bps, "
                + "interest_amount_cents = excluded.interest_amount_cents, "
                + "total_amount_cents = excluded.total_amount_cents, "
                + "prepared_at = strftime('%Y-%m-%dT%H:%M:%S', 'now')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, settlement.personId());
            ps.setInt(2, settlement.year());
            ps.setLong(3, settlement.savingsTotal().toCents());
            ps.setInt(4, settlement.interestRateBps());
            ps.setLong(5, settlement.interestAmount().toCents());
            ps.setLong(6, settlement.totalAmount().toCents());
            ps.executeUpdate();
            return findByPersonAndYear(settlement.personId(), settlement.year()).orElseThrow();
        } catch (SQLException e) {
            log.error("Error al preparar liquidación de la persona {} para {}", settlement.personId(), settlement.year(), e);
            throw new DataAccessException("No fue posible preparar la liquidación.", e);
        }
    }

    @Override
    public Optional<Settlement> findByPersonAndYear(long personId, int year) {
        String sql = "SELECT * FROM settlements WHERE person_id = ? AND year = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, personId);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapSettlement(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Error al consultar liquidación de la persona {} para {}", personId, year, e);
            throw new DataAccessException("No fue posible consultar la liquidación.", e);
        }
    }

    @Override
    public List<Settlement> findByPerson(long personId) {
        String sql = "SELECT * FROM settlements WHERE person_id = ? ORDER BY year DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Settlement> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapSettlement(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al consultar liquidaciones de la persona {}", personId, e);
            throw new DataAccessException("No fue posible consultar las liquidaciones.", e);
        }
    }

    private Settlement mapSettlement(ResultSet rs) throws SQLException {
        return new Settlement(
                rs.getLong("id"),
                rs.getLong("person_id"),
                rs.getInt("year"),
                Money.ofCents(rs.getLong("savings_total_cents")),
                rs.getInt("interest_rate_bps"),
                Money.ofCents(rs.getLong("interest_amount_cents")),
                Money.ofCents(rs.getLong("total_amount_cents")),
                LocalDateTime.parse(rs.getString("prepared_at")));
    }
}
