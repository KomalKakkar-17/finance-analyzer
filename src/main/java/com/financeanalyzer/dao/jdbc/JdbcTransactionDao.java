package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.Expense;
import com.financeanalyzer.model.Income;
import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of TransactionDao.
 * Demonstrates Unit 4 DAO pattern, parameterized queries, and polymorphic entity mapping.
 */
public class JdbcTransactionDao implements TransactionDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcTransactionDao.class);

    @Override
    public Optional<Transaction> findById(Long id) {
        String sql = """
            SELECT t.id, t.user_id, t.category_id, c.name AS category_name, t.type,
                   t.amount_paise, t.txn_date, t.payment_mode, t.notes, t.recurring_rule_id, t.created_at
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding transaction by id: " + id, e);
            throw new DataAccessException("Failed to query transaction.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transaction> findAll(Long userId) {
        String sql = """
            SELECT t.id, t.user_id, t.category_id, c.name AS category_name, t.type,
                   t.amount_paise, t.txn_date, t.payment_mode, t.notes, t.recurring_rule_id, t.created_at
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = ?
            ORDER BY t.txn_date DESC, t.id DESC
        """;
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying all transactions for user: " + userId, e);
            throw new DataAccessException("Failed to query transactions.", e);
        }
        return list;
    }

    @Override
    public List<Transaction> findFiltered(Long userId, LocalDate fromDate, LocalDate toDate,
                                          Long categoryId, TransactionType type,
                                          BigDecimal minAmount, BigDecimal maxAmount, String keyword) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.id, t.user_id, t.category_id, c.name AS category_name, t.type,
                   t.amount_paise, t.txn_date, t.payment_mode, t.notes, t.recurring_rule_id, t.created_at
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (fromDate != null) {
            sql.append(" AND t.txn_date >= ?");
            params.add(DateUtil.toIsoString(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND t.txn_date <= ?");
            params.add(DateUtil.toIsoString(toDate));
        }
        if (categoryId != null) {
            sql.append(" AND t.category_id = ?");
            params.add(categoryId);
        }
        if (type != null) {
            sql.append(" AND t.type = ?");
            params.add(type.name());
        }
        if (minAmount != null) {
            sql.append(" AND t.amount_paise >= ?");
            params.add(MoneyUtil.toPaise(minAmount));
        }
        if (maxAmount != null) {
            sql.append(" AND t.amount_paise <= ?");
            params.add(MoneyUtil.toPaise(maxAmount));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(t.notes) LIKE ? OR LOWER(c.name) LIKE ?)");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }

        sql.append(" ORDER BY t.txn_date DESC, t.id DESC");

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in filtered transaction search.", e);
            throw new DataAccessException("Failed to perform filtered transaction search.", e);
        }
        return list;
    }

    @Override
    public Transaction save(Transaction transaction) {
        String sql = """
            INSERT INTO transactions (user_id, category_id, type, amount_paise, txn_date, payment_mode, notes, recurring_rule_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, transaction.getUserId());
            ps.setLong(2, transaction.getCategoryId());
            ps.setString(3, transaction.getType().name());
            ps.setLong(4, MoneyUtil.toPaise(transaction.getAmount()));
            ps.setString(5, DateUtil.toIsoString(transaction.getDate()));
            ps.setString(6, transaction.getPaymentMode());
            ps.setString(7, transaction.getNotes());
            if (transaction.getRecurringRuleId() != null) {
                ps.setLong(8, transaction.getRecurringRuleId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setString(9, transaction.getCreatedAt() != null ? transaction.getCreatedAt().toString() : LocalDateTime.now().toString());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    transaction.setId(rs.getLong(1));
                }
            }
            return transaction;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving transaction.", e);
            throw new DataAccessException("Failed to persist transaction.", e);
        }
    }

    @Override
    public boolean update(Transaction transaction) {
        String sql = """
            UPDATE transactions
            SET category_id = ?, type = ?, amount_paise = ?, txn_date = ?, payment_mode = ?, notes = ?
            WHERE id = ? AND user_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transaction.getCategoryId());
            ps.setString(2, transaction.getType().name());
            ps.setLong(3, MoneyUtil.toPaise(transaction.getAmount()));
            ps.setString(4, DateUtil.toIsoString(transaction.getDate()));
            ps.setString(5, transaction.getPaymentMode());
            ps.setString(6, transaction.getNotes());
            ps.setLong(7, transaction.getId());
            ps.setLong(8, transaction.getUserId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating transaction id: " + transaction.getId(), e);
            throw new DataAccessException("Failed to update transaction.", e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long userId) {
        String sql = "DELETE FROM transactions WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting transaction id: " + id, e);
            throw new DataAccessException("Failed to delete transaction.", e);
        }
    }

    @Override
    public BigDecimal getCategorySpendInMonth(Long userId, Long categoryId, YearMonth yearMonth) {
        String prefix = yearMonth.toString() + "%";
        String sql = "SELECT COALESCE(SUM(amount_paise), 0) FROM transactions WHERE user_id = ? AND category_id = ? AND type = 'EXPENSE' AND txn_date LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, categoryId);
            ps.setString(3, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return MoneyUtil.toRupees(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting category spend for category: " + categoryId, e);
            throw new DataAccessException("Failed to aggregate category spend.", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalExpenseInMonth(Long userId, YearMonth yearMonth) {
        String prefix = yearMonth.toString() + "%";
        String sql = "SELECT COALESCE(SUM(amount_paise), 0) FROM transactions WHERE user_id = ? AND type = 'EXPENSE' AND txn_date LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return MoneyUtil.toRupees(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total monthly expense.", e);
            throw new DataAccessException("Failed to aggregate total expense.", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalIncomeInMonth(Long userId, YearMonth yearMonth) {
        String prefix = yearMonth.toString() + "%";
        String sql = "SELECT COALESCE(SUM(amount_paise), 0) FROM transactions WHERE user_id = ? AND type = 'INCOME' AND txn_date LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return MoneyUtil.toRupees(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total monthly income.", e);
            throw new DataAccessException("Failed to aggregate total income.", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Map<String, BigDecimal> getCategoryExpenseBreakdown(Long userId, YearMonth yearMonth) {
        String prefix = yearMonth.toString() + "%";
        String sql = """
            SELECT c.name, COALESCE(SUM(t.amount_paise), 0) AS total_paise
            FROM transactions t
            JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = ? AND t.type = 'EXPENSE' AND t.txn_date LIKE ?
            GROUP BY c.name
            ORDER BY total_paise DESC
        """;
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String catName = rs.getString("name");
                    BigDecimal amount = MoneyUtil.toRupees(rs.getLong("total_paise"));
                    map.put(catName, amount);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting category expense breakdown.", e);
            throw new DataAccessException("Failed to retrieve category breakdown.", e);
        }
        return map;
    }

    @Override
    public Map<Long, BigDecimal> getCategoryExpenseBreakdownById(Long userId, YearMonth yearMonth) {
        String prefix = yearMonth.toString() + "%";
        String sql = """
            SELECT c.id, COALESCE(SUM(t.amount_paise), 0) AS total_paise
            FROM transactions t
            JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = ? AND t.type = 'EXPENSE' AND t.txn_date LIKE ?
            GROUP BY c.id
            ORDER BY total_paise DESC
        """;
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getLong("id"), MoneyUtil.toRupees(rs.getLong("total_paise")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting category expense breakdown by ID.", e);
            throw new DataAccessException("Failed to retrieve category breakdown by ID.", e);
        }
        return map;
    }

    @Override
    public List<Transaction> findRecent(Long userId, int limit) {
        String sql = """
            SELECT t.id, t.user_id, t.category_id, c.name AS category_name, t.type,
                   t.amount_paise, t.txn_date, t.payment_mode, t.notes, t.recurring_rule_id, t.created_at
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.user_id = ?
            ORDER BY t.txn_date DESC, t.id DESC
            LIMIT ?
        """;
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding recent transactions.", e);
            throw new DataAccessException("Failed to query recent transactions.", e);
        }
        return list;
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long userId = rs.getLong("user_id");
        Long categoryId = rs.getLong("category_id");
        String categoryName = rs.getString("category_name");
        TransactionType type = TransactionType.valueOf(rs.getString("type"));
        BigDecimal amount = MoneyUtil.toRupees(rs.getLong("amount_paise"));
        LocalDate date = DateUtil.parseIsoDate(rs.getString("txn_date"));
        String paymentMode = rs.getString("payment_mode");
        String notes = rs.getString("notes");
        Long ruleId = rs.getObject("recurring_rule_id") != null ? rs.getLong("recurring_rule_id") : null;
        String createdAtStr = rs.getString("created_at");
        LocalDateTime createdAt = (createdAtStr != null && !createdAtStr.isEmpty()) ?
                LocalDateTime.parse(createdAtStr) : LocalDateTime.now();

        if (type == TransactionType.INCOME) {
            return new Income(id, userId, categoryId, categoryName, amount, date, paymentMode, notes, ruleId, createdAt);
        } else {
            return new Expense(id, userId, categoryId, categoryName, amount, date, paymentMode, notes, ruleId, createdAt);
        }
    }
}
