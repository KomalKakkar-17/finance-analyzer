package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.FixedCategoryDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.util.AppLogger;

import java.sql.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of {@link FixedCategoryDao}.
 * Operates on the {@code user_fixed_categories} join table.
 *
 * <p>All SQL is confined to this class (strict DAO layer rule).</p>
 */
public class JdbcFixedCategoryDao implements FixedCategoryDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcFixedCategoryDao.class);

    // ------------------------------------------------------------------ queries

    private static final String SQL_GET_NAMES =
            """
            SELECT c.name
              FROM user_fixed_categories ufc
              JOIN categories c ON c.id = ufc.category_id
             WHERE ufc.user_id = ?
            """;

    private static final String SQL_GET_IDS =
            "SELECT category_id FROM user_fixed_categories WHERE user_id = ?";

    private static final String SQL_ADD =
            "INSERT OR IGNORE INTO user_fixed_categories (user_id, category_id) VALUES (?, ?)";

    private static final String SQL_REMOVE =
            "DELETE FROM user_fixed_categories WHERE user_id = ? AND category_id = ?";

    private static final String SQL_IS_FIXED =
            "SELECT 1 FROM user_fixed_categories WHERE user_id = ? AND category_id = ? LIMIT 1";

    // ------------------------------------------------------------------

    @Override
    public Set<String> getFixedCategoryNames(Long userId) {
        Set<String> names = new HashSet<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_NAMES)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching fixed category names for userId=" + userId, e);
            throw new DataAccessException("Failed to fetch fixed categories.", e);
        }
        return Collections.unmodifiableSet(names);
    }

    @Override
    public Set<Long> getFixedCategoryIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_IDS)) {
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
    public void addFixedCategory(Long userId, Long categoryId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ADD)) {
            ps.setLong(1, userId);
            ps.setLong(2, categoryId);
            ps.executeUpdate();
            LOGGER.info("Marked category " + categoryId + " as fixed for user " + userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding fixed category", e);
            throw new DataAccessException("Failed to add fixed category.", e);
        }
    }

    @Override
    public void removeFixedCategory(Long userId, Long categoryId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_REMOVE)) {
            ps.setLong(1, userId);
            ps.setLong(2, categoryId);
            ps.executeUpdate();
            LOGGER.info("Removed fixed flag for category " + categoryId + " for user " + userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error removing fixed category", e);
            throw new DataAccessException("Failed to remove fixed category.", e);
        }
    }

    @Override
    public boolean isFixed(Long userId, Long categoryId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_IS_FIXED)) {
            ps.setLong(1, userId);
            ps.setLong(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if category is fixed", e);
            throw new DataAccessException("Failed to check fixed category.", e);
        }
    }
}
