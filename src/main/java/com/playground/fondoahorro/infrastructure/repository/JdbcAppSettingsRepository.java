package com.playground.fondoahorro.infrastructure.repository;

import com.playground.fondoahorro.domain.outputport.AppSettingsRepository;
import com.playground.fondoahorro.infrastructure.exception.DataAccessException;
import com.playground.fondoahorro.infrastructure.config.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcAppSettingsRepository implements AppSettingsRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAppSettingsRepository.class);

    @Override
    public Optional<String> get(String key) {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("value")) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Error al leer configuración {}", key, e);
            throw new DataAccessException("No fue posible leer la configuración.", e);
        }
    }

    @Override
    public void set(String key, String value) {
        String sql = "UPDATE app_settings SET value = ?, updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now') WHERE key = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, key);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("La configuración \"" + key + "\" no existe.");
            }
        } catch (SQLException e) {
            log.error("Error al guardar configuración {}", key, e);
            throw new DataAccessException("No fue posible guardar la configuración.", e);
        }
    }
}
