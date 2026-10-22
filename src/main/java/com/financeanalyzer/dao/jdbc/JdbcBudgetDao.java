package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.BudgetDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.Budget;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of BudgetDao.
 */
public class JdbcBudgetDao implements BudgetDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcBudgetDao.class);

    @Override
    public Optional<Budget> findById(Long id) {
        String sql = """
            SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.year_month, b.limit_paise
            FROM budgets b
            JOIN categories c ON b.category_id = c.id
            WHERE b.id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding budget by id: " + id, e);
            throw new DataAccessException("Failed to query budget.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Budget> findAll(Long userId) {
        String sql = """
            SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.year_month, b.limit_paise
            FROM budgets b
            JOIN categories c ON b.category_id = c.id
            WHERE b.user_id = ?
            ORDER BY b.year_month DESC, c.name ASC
        """;
        List<Budget> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying all budgets for user: " + userId, e);
            throw new DataAccessException("Failed to query budgets.", e);
        }
        return list;
    }

    @Override
    public Optional<Budget> findByCategoryAndMonth(Long userId, Long categoryId, YearMonth yearMonth) {
        String sql = """
            SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.year_month, b.limit_paise
            FROM budgets b
            JOIN categories c ON b.category_id = c.id
            WHERE b.user_id = ? AND b.category_id = ? AND b.year_month = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, categoryId);
            ps.setString(3, DateUtil.toMonthString(yearMonth));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding budget for category " + categoryId + " and month " + yearMonth, e);
            throw new DataAccessException("Failed to query budget by month.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Budget> findAllForMonth(Long userId, YearMonth yearMonth) {
        String sql = """
            SELECT b.id, b.user_id, b.category_id, c.name AS category_name, b.year_month, b.limit_paise
            FROM budgets b
            JOIN categories c ON b.category_id = c.id
            WHERE b.user_id = ? AND b.year_month = ?
            ORDER BY c.name ASC
        """;
        List<Budget> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, DateUtil.toMonthString(yearMonth));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying budgets for month: " + yearMonth, e);
            throw new DataAccessException("Failed to query budgets for month.", e);
        }
        return list;
    }

    @Override
    public Budget save(Budget budget) {
        String sql = """
            INSERT INTO budgets (user_id, category_id, year_month, limit_paise)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(user_id, category_id, year_month) DO UPDATE SET limit_paise = excluded.limit_paise
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, budget.getUserId());
            ps.setLong(2, budget.getCategoryId());
            ps.setString(3, DateUtil.toMonthString(budget.getYearMonth()));
            ps.setLong(4, MoneyUtil.toPaise(budget.getLimit()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    budget.setId(rs.getLong(1));
                }
            }
            return budget;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving budget.", e);
            throw new DataAccessException("Failed to save budget.", e);
        }
    }

    @Override
    public boolean update(Budget budget) {
        String sql = "UPDATE budgets SET limit_paise = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, MoneyUtil.toPaise(budget.getLimit()));
            ps.setLong(2, budget.getId());
            ps.setLong(3, budget.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating budget id: " + budget.getId(), e);
            throw new DataAccessException("Failed to update budget.", e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long userId) {
        String sql = "DELETE FROM budgets WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting budget id: " + id, e);
            throw new DataAccessException("Failed to delete budget.", e);
        }
    }

    private Budget mapRowToBudget(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long userId = rs.getLong("user_id");
        Long categoryId = rs.getLong("category_id");
        String categoryName = rs.getString("category_name");
        YearMonth yearMonth = DateUtil.parseYearMonth(rs.getString("year_month"));
        BigDecimal limit = MoneyUtil.toRupees(rs.getLong("limit_paise"));

        return new Budget(id, userId, categoryId, categoryName, yearMonth, limit);
    }
}
