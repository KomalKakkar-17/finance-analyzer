package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.UserFixedCategoryDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.ConfigLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation for per-user fixed expense categories.
 */
public class JdbcUserFixedCategoryDao implements UserFixedCategoryDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcUserFixedCategoryDao.class);

    private static final String SQL_FIND_IDS =
            "SELECT category_id FROM user_fixed_categories WHERE user_id = ?";
    private static final String SQL_INSERT_FIXED =
            "INSERT OR IGNORE INTO user_fixed_categories (user_id, category_id) VALUES (?, ?)";
    private static final String SQL_DELETE_FIXED =
            "DELETE FROM user_fixed_categories WHERE user_id = ? AND category_id = ?";
    private static final String SQL_INSERT_INITIALIZED =
            "INSERT OR IGNORE INTO user_fixed_category_initializations (user_id) VALUES (?)";

    @Override
    public Set<Long> findFixedCategoryIds(long userId) {
        Set<Long> ids = new HashSet<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_IDS)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("category_id"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching fixed category IDs for userId=" + userId, e);
            throw new DataAccessException("Failed to fetch fixed category IDs.", e);
        }
        return Collections.unmodifiableSet(ids);
    }

    @Override
    public void setFixed(long userId, long categoryId, boolean fixed) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = fixed ? SQL_INSERT_FIXED : SQL_DELETE_FIXED;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, categoryId);
                    ps.executeUpdate();
                }
                markInitialized(conn, userId);
                conn.commit();
            } catch (SQLException e) {
                rollback(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating fixed category for userId=" + userId, e);
            throw new DataAccessException("Failed to update fixed category.", e);
        }
    }

    @Override
    public void replaceAll(long userId, Set<Long> categoryIds) {
        Objects.requireNonNull(categoryIds, "Category IDs must not be null.");
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement delete = conn.prepareStatement(
                        "DELETE FROM user_fixed_categories WHERE user_id = ?")) {
                    delete.setLong(1, userId);
                    delete.executeUpdate();
                }

                try (PreparedStatement insert = conn.prepareStatement(SQL_INSERT_FIXED)) {
                    for (Long categoryId : categoryIds) {
                        insert.setLong(1, userId);
                        insert.setLong(2, Objects.requireNonNull(categoryId, "Category IDs must not contain null."));
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }

                markInitialized(conn, userId);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error replacing fixed categories for userId=" + userId, e);
            throw new DataAccessException("Failed to replace fixed categories.", e);
        }
    }

    @Override
    public void initializeDefaultsIfNeeded(long userId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                initializeDefaultsIfNeeded(conn, userId);
                conn.commit();
            } catch (SQLException e) {
                rollback(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing fixed categories for userId=" + userId, e);
            throw new DataAccessException("Failed to initialize fixed categories.", e);
        }
    }

    static void initializeDefaultsIfNeeded(Connection conn, long userId) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM user_fixed_category_initializations WHERE user_id = ?")) {
            check.setLong(1, userId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement check = conn.prepareStatement(SQL_FIND_IDS)) {
            check.setLong(1, userId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    markInitialized(conn, userId);
                    return;
                }
            }
        }

        String configuredDefaults = ConfigLoader.getProperty("default.fixed.categories", "Rent,Utilities");
        if (configuredDefaults != null && !configuredDefaults.isBlank()) {
            String sql = """
                    INSERT OR IGNORE INTO user_fixed_categories (user_id, category_id)
                    SELECT ?, id FROM categories
                    WHERE name = ? AND type = 'EXPENSE' AND user_id IS NULL
                    LIMIT 1
                    """;
            try (PreparedStatement insert = conn.prepareStatement(sql)) {
                for (String name : configuredDefaults.split(",")) {
                    if (name.isBlank()) {
                        continue;
                    }
                    insert.setLong(1, userId);
                    insert.setString(2, name.trim());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
        markInitialized(conn, userId);
        LOGGER.info("Initialized default fixed categories for userId=" + userId);
    }

    private static void markInitialized(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_INITIALIZED)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    private static void rollback(Connection conn, Exception cause) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }
}
