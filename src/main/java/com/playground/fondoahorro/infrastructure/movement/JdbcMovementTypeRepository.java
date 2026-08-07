package com.playground.fondoahorro.infrastructure.movement;

import com.playground.fondoahorro.domain.movement.MovementKind;
import com.playground.fondoahorro.domain.movement.MovementType;
import com.playground.fondoahorro.domain.movement.MovementTypeRepository;
import com.playground.fondoahorro.infrastructure.database.DataAccessException;
import com.playground.fondoahorro.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcMovementTypeRepository implements MovementTypeRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcMovementTypeRepository.class);

    @Override
    public MovementType insert(MovementType type) {
        String sql = "INSERT INTO movement_types (name, kind, code, active) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, type.name());
            ps.setString(2, type.kind().name());
            ps.setString(3, type.code());
            ps.setInt(4, type.active() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            log.error("Error al crear tipo de movimiento", e);
            throw new DataAccessException("No fue posible crear el tipo.", e);
        }
    }

    @Override
    public void rename(long id, String newName) {
        String sql = "UPDATE movement_types SET name = ?, updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now') WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al renombrar tipo de movimiento {}", id, e);
            throw new DataAccessException("No fue posible renombrar el tipo.", e);
        }
    }

    @Override
    public void setActive(long id, boolean active) {
        String sql = "UPDATE movement_types SET active = ?, updated_at = strftime('%Y-%m-%dT%H:%M:%S', 'now') WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error al cambiar estado del tipo de movimiento {}", id, e);
            throw new DataAccessException("No fue posible actualizar el estado del tipo.", e);
        }
    }

    @Override
    public Optional<MovementType> findById(long id) {
        return findOne("SELECT * FROM movement_types WHERE id = ?", id);
    }

    @Override
    public Optional<MovementType> findByCode(String code) {
        String sql = "SELECT * FROM movement_types WHERE code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Error al buscar tipo de movimiento por código {}", code, e);
            throw new DataAccessException("No fue posible consultar el tipo.", e);
        }
    }

    @Override
    public List<MovementType> findAll(boolean includeInactive) {
        StringBuilder sql = new StringBuilder("SELECT * FROM movement_types WHERE 1 = 1");
        if (!includeInactive) {
            sql.append(" AND active = 1");
        }
        sql.append(" ORDER BY kind, name");
        return query(sql.toString());
    }

    @Override
    public List<MovementType> findByKind(MovementKind kind, boolean includeInactive) {
        StringBuilder sql = new StringBuilder("SELECT * FROM movement_types WHERE kind = ?");
        if (!includeInactive) {
            sql.append(" AND active = 1");
        }
        sql.append(" ORDER BY name");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, kind.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<MovementType> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            log.error("Error al listar tipos de movimiento por clase {}", kind, e);
            throw new DataAccessException("No fue posible consultar los tipos.", e);
        }
    }

    private Optional<MovementType> findOne(String sql, long id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Error al consultar tipo de movimiento {}", id, e);
            throw new DataAccessException("No fue posible consultar el tipo.", e);
        }
    }

    private List<MovementType> query(String sql) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<MovementType> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            log.error("Error al listar tipos de movimiento", e);
            throw new DataAccessException("No fue posible consultar los tipos.", e);
        }
    }

    private MovementType mapRow(ResultSet rs) throws SQLException {
        return new MovementType(
                rs.getLong("id"),
                rs.getString("name"),
                MovementKind.valueOf(rs.getString("kind")),
                rs.getString("code"),
                rs.getInt("active") == 1,
                LocalDateTime.parse(rs.getString("created_at")),
                LocalDateTime.parse(rs.getString("updated_at")));
    }
}
