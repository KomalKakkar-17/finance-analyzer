package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.AuditLogDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.AuditLog;
import com.financeanalyzer.util.AppLogger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of AuditLogDao.
 */
public class JdbcAuditLogDao implements AuditLogDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcAuditLogDao.class);

    @Override
    public Optional<AuditLog> findById(Long id) {
        String sql = "SELECT id, rule_id, run_at, result, message FROM execution_audit_log WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding audit log by id: " + id, e);
            throw new DataAccessException("Failed to query audit log.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<AuditLog> findAll(Long userId) {
        String sql = """
            SELECT a.id, a.rule_id, a.run_at, a.result, a.message
            FROM execution_audit_log a
            JOIN recurring_rules r ON a.rule_id = r.id
            WHERE r.user_id = ?
            ORDER BY a.run_at DESC
        """;
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying all audit logs for user: " + userId, e);
            throw new DataAccessException("Failed to query audit logs.", e);
        }
        return list;
    }

    @Override
    public List<AuditLog> findByRuleId(Long ruleId) {
        String sql = "SELECT id, rule_id, run_at, result, message FROM execution_audit_log WHERE rule_id = ? ORDER BY run_at DESC";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ruleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying audit logs by ruleId: " + ruleId, e);
            throw new DataAccessException("Failed to query audit logs.", e);
        }
        return list;
    }

    @Override
    public AuditLog save(AuditLog log) {
        String sql = "INSERT INTO execution_audit_log (rule_id, run_at, result, message) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, log.getRuleId());
            ps.setString(2, log.getRunAt() != null ? log.getRunAt().toString() : LocalDateTime.now().toString());
            ps.setString(3, log.getResult());
            ps.setString(4, log.getMessage());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getLong(1));
                }
            }
            return log;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving audit log.", e);
            throw new DataAccessException("Failed to save audit log.", e);
        }
    }

    @Override
    public boolean update(AuditLog log) {
        return false; // Audit logs are immutable
    }

    @Override
    public boolean deleteById(Long id, Long userId) {
        return false; // Audit logs are immutable
    }

    private AuditLog mapRowToAuditLog(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long ruleId = rs.getLong("rule_id");
        String runAtStr = rs.getString("run_at");
        LocalDateTime runAt = (runAtStr != null && !runAtStr.isEmpty()) ?
                LocalDateTime.parse(runAtStr) : LocalDateTime.now();
        String result = rs.getString("result");
        String message = rs.getString("message");

        return new AuditLog(id, ruleId, runAt, result, message);
    }
}
