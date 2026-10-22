package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.RecurringTransactionDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.RecurringTransaction;
import com.financeanalyzer.model.enums.Frequency;
import com.financeanalyzer.model.enums.RuleStatus;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of RecurringTransactionDao.
 */
public class JdbcRecurringTransactionDao implements RecurringTransactionDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcRecurringTransactionDao.class);

    @Override
    public Optional<RecurringTransaction> findById(Long id) {
        String sql = """
            SELECT r.id, r.user_id, r.category_id, c.name AS category_name, r.type,
                   r.amount_paise, r.frequency, r.start_date, r.next_due_date, r.end_date, r.status
            FROM recurring_rules r
            LEFT JOIN categories c ON r.category_id = c.id
            WHERE r.id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRule(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding recurring rule by id: " + id, e);
            throw new DataAccessException("Failed to query recurring rule.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<RecurringTransaction> findAll(Long userId) {
        String sql = """
            SELECT r.id, r.user_id, r.category_id, c.name AS category_name, r.type,
                   r.amount_paise, r.frequency, r.start_date, r.next_due_date, r.end_date, r.status
            FROM recurring_rules r
            LEFT JOIN categories c ON r.category_id = c.id
            WHERE r.user_id = ?
            ORDER BY r.status ASC, r.next_due_date ASC
        """;
        List<RecurringTransaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRule(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying recurring rules for user: " + userId, e);
            throw new DataAccessException("Failed to query recurring rules.", e);
        }
        return list;
    }

    @Override
    public List<RecurringTransaction> findDueRules(LocalDate asOfDate) {
        String sql = """
            SELECT r.id, r.user_id, r.category_id, c.name AS category_name, r.type,
                   r.amount_paise, r.frequency, r.start_date, r.next_due_date, r.end_date, r.status
            FROM recurring_rules r
            LEFT JOIN categories c ON r.category_id = c.id
            WHERE r.status = 'ACTIVE' AND r.next_due_date <= ?
        """;
        List<RecurringTransaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateUtil.toIsoString(asOfDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRule(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying due recurring rules.", e);
            throw new DataAccessException("Failed to query due recurring rules.", e);
        }
        return list;
    }

    @Override
    public RecurringTransaction save(RecurringTransaction rule) {
        String sql = """
            INSERT INTO recurring_rules (user_id, category_id, type, amount_paise, frequency, start_date, next_due_date, end_date, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rule.getUserId());
            ps.setLong(2, rule.getCategoryId());
            ps.setString(3, rule.getType().name());
            ps.setLong(4, MoneyUtil.toPaise(rule.getAmount()));
            ps.setString(5, rule.getFrequency().name());
            ps.setString(6, DateUtil.toIsoString(rule.getStartDate()));
            ps.setString(7, DateUtil.toIsoString(rule.getNextDueDate()));
            if (rule.getEndDate() != null) {
                ps.setString(8, DateUtil.toIsoString(rule.getEndDate()));
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            ps.setString(9, rule.getStatus().name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    rule.setId(rs.getLong(1));
                }
            }
            return rule;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting recurring rule.", e);
            throw new DataAccessException("Failed to save recurring rule.", e);
        }
    }

    @Override
    public boolean update(RecurringTransaction rule) {
        String sql = """
            UPDATE recurring_rules
            SET category_id = ?, type = ?, amount_paise = ?, frequency = ?, next_due_date = ?, end_date = ?, status = ?
            WHERE id = ? AND user_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rule.getCategoryId());
            ps.setString(2, rule.getType().name());
            ps.setLong(3, MoneyUtil.toPaise(rule.getAmount()));
            ps.setString(4, rule.getFrequency().name());
            ps.setString(5, DateUtil.toIsoString(rule.getNextDueDate()));
            if (rule.getEndDate() != null) {
                ps.setString(6, DateUtil.toIsoString(rule.getEndDate()));
            } else {
                ps.setNull(6, Types.VARCHAR);
            }
            ps.setString(7, rule.getStatus().name());
            ps.setLong(8, rule.getId());
            ps.setLong(9, rule.getUserId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating recurring rule id: " + rule.getId(), e);
            throw new DataAccessException("Failed to update recurring rule.", e);
        }
    }

    @Override
    public boolean updateStatus(Long ruleId, Long userId, RuleStatus status) {
        String sql = "UPDATE recurring_rules SET status = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, ruleId);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating status for rule: " + ruleId, e);
            throw new DataAccessException("Failed to update recurring rule status.", e);
        }
    }

    @Override
    public boolean advanceNextDueDate(Long ruleId, LocalDate nextDueDate, RuleStatus newStatus) {
        String sql = "UPDATE recurring_rules SET next_due_date = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateUtil.toIsoString(nextDueDate));
            ps.setString(2, newStatus.name());
            ps.setLong(3, ruleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error advancing next due date for rule: " + ruleId, e);
            throw new DataAccessException("Failed to advance recurring rule schedule.", e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long userId) {
        String sql = "DELETE FROM recurring_rules WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting recurring rule: " + id, e);
            throw new DataAccessException("Failed to delete recurring rule.", e);
        }
    }

    private RecurringTransaction mapRowToRule(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long userId = rs.getLong("user_id");
        Long categoryId = rs.getLong("category_id");
        String categoryName = rs.getString("category_name");
        TransactionType type = TransactionType.valueOf(rs.getString("type"));
        BigDecimal amount = MoneyUtil.toRupees(rs.getLong("amount_paise"));
        Frequency frequency = Frequency.valueOf(rs.getString("frequency"));
        LocalDate startDate = DateUtil.parseIsoDate(rs.getString("start_date"));
        LocalDate nextDueDate = DateUtil.parseIsoDate(rs.getString("next_due_date"));
        LocalDate endDate = DateUtil.parseIsoDate(rs.getString("end_date"));
        RuleStatus status = RuleStatus.valueOf(rs.getString("status"));

        return new RecurringTransaction(id, userId, categoryId, categoryName, type, amount, frequency, startDate, nextDueDate, endDate, status);
    }
}
