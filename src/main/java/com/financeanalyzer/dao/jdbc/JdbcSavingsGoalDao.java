package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.SavingsGoalDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.SavingsGoal;
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
 * JDBC implementation of SavingsGoalDao.
 */
public class JdbcSavingsGoalDao implements SavingsGoalDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcSavingsGoalDao.class);

    @Override
    public Optional<SavingsGoal> findById(Long id) {
        String sql = "SELECT id, user_id, title, target_paise, saved_paise, deadline, allocation_ratio, is_active FROM savings_goals WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToGoal(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding savings goal by id: " + id, e);
            throw new DataAccessException("Failed to query savings goal.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<SavingsGoal> findAll(Long userId) {
        String sql = "SELECT id, user_id, title, target_paise, saved_paise, deadline, allocation_ratio, is_active FROM savings_goals WHERE user_id = ? ORDER BY is_active DESC, deadline ASC";
        List<SavingsGoal> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToGoal(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying all savings goals for user: " + userId, e);
            throw new DataAccessException("Failed to query savings goals.", e);
        }
        return list;
    }

    @Override
    public List<SavingsGoal> findActiveGoals(Long userId) {
        String sql = "SELECT id, user_id, title, target_paise, saved_paise, deadline, allocation_ratio, is_active FROM savings_goals WHERE user_id = ? AND is_active = 1 ORDER BY deadline ASC";
        List<SavingsGoal> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToGoal(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying active savings goals for user: " + userId, e);
            throw new DataAccessException("Failed to query active savings goals.", e);
        }
        return list;
    }

    @Override
    public SavingsGoal save(SavingsGoal goal) {
        String sql = """
            INSERT INTO savings_goals (user_id, title, target_paise, saved_paise, deadline, allocation_ratio, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, goal.getUserId());
            ps.setString(2, goal.getTitle().trim());
            ps.setLong(3, MoneyUtil.toPaise(goal.getTargetAmount()));
            ps.setLong(4, MoneyUtil.toPaise(goal.getSavedAmount()));
            ps.setString(5, DateUtil.toIsoString(goal.getDeadline()));
            ps.setInt(6, goal.getAllocationRatio());
            ps.setInt(7, goal.isActive() ? 1 : 0);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    goal.setId(rs.getLong(1));
                }
            }
            return goal;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving savings goal: " + goal.getTitle(), e);
            throw new DataAccessException("Failed to save savings goal.", e);
        }
    }

    @Override
    public boolean update(SavingsGoal goal) {
        String sql = """
            UPDATE savings_goals
            SET title = ?, target_paise = ?, saved_paise = ?, deadline = ?, allocation_ratio = ?, is_active = ?
            WHERE id = ? AND user_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goal.getTitle().trim());
            ps.setLong(2, MoneyUtil.toPaise(goal.getTargetAmount()));
            ps.setLong(3, MoneyUtil.toPaise(goal.getSavedAmount()));
            ps.setString(4, DateUtil.toIsoString(goal.getDeadline()));
            ps.setInt(5, goal.getAllocationRatio());
            ps.setInt(6, goal.isActive() ? 1 : 0);
            ps.setLong(7, goal.getId());
            ps.setLong(8, goal.getUserId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating savings goal id: " + goal.getId(), e);
            throw new DataAccessException("Failed to update savings goal.", e);
        }
    }

    @Override
    public boolean addFunds(Long goalId, Long userId, BigDecimal amount) {
        String sql = "UPDATE savings_goals SET saved_paise = saved_paise + ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, MoneyUtil.toPaise(amount));
            ps.setLong(2, goalId);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding funds to goal id: " + goalId, e);
            throw new DataAccessException("Failed to add funds to savings goal.", e);
        }
    }

    @Override
    public boolean updateAllocationRatio(Long goalId, Long userId, int newRatio) {
        String sql = "UPDATE savings_goals SET allocation_ratio = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newRatio);
            ps.setLong(2, goalId);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating allocation ratio for goal: " + goalId, e);
            throw new DataAccessException("Failed to update allocation ratio.", e);
        }
    }

    @Override
    public boolean updateActiveStatus(Long goalId, Long userId, boolean active) {
        String sql = "UPDATE savings_goals SET is_active = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, goalId);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating active status for goal: " + goalId, e);
            throw new DataAccessException("Failed to update goal status.", e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long userId) {
        String sql = "DELETE FROM savings_goals WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting savings goal: " + id, e);
            throw new DataAccessException("Failed to delete savings goal.", e);
        }
    }

    private SavingsGoal mapRowToGoal(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long userId = rs.getLong("user_id");
        String title = rs.getString("title");
        BigDecimal target = MoneyUtil.toRupees(rs.getLong("target_paise"));
        BigDecimal saved = MoneyUtil.toRupees(rs.getLong("saved_paise"));
        LocalDate deadline = DateUtil.parseIsoDate(rs.getString("deadline"));
        int ratio = rs.getInt("allocation_ratio");
        boolean active = rs.getInt("is_active") == 1;

        return new SavingsGoal(id, userId, title, target, saved, deadline, ratio, active);
    }
}
