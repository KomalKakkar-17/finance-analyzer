package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of CategoryDao.
 * Queries both system-wide default categories and user-specific custom categories.
 */
public class JdbcCategoryDao implements CategoryDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcCategoryDao.class);

    @Override
    public Optional<Category> findById(Long id) {
        String sql = "SELECT id, user_id, name, type, is_system FROM categories WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding category by id: " + id, e);
            throw new DataAccessException("Failed to query category.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Category> findAll(Long userId) {
        return findCategoriesForUser(userId, null);
    }

    @Override
    public List<Category> findCategoriesForUser(Long userId, TransactionType type) {
        StringBuilder sql = new StringBuilder("SELECT id, user_id, name, type, is_system FROM categories WHERE (user_id IS NULL");
        if (userId != null) {
            sql.append(" OR user_id = ?");
        }
        sql.append(")");
        if (type != null) {
            sql.append(" AND type = ?");
        }
        sql.append(" ORDER BY is_system DESC, name ASC");

        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (userId != null) {
                ps.setLong(paramIdx++, userId);
            }
            if (type != null) {
                ps.setString(paramIdx++, type.name());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding categories for user: " + userId, e);
            throw new DataAccessException("Failed to query user categories.", e);
        }
        return list;
    }

    @Override
    public Optional<Category> findByName(Long userId, String name, TransactionType type) {
        if (name == null) return Optional.empty();
        String sql = "SELECT id, user_id, name, type, is_system FROM categories " +
                "WHERE LOWER(name) = LOWER(?) AND type = ? AND (user_id IS NULL OR user_id = ?) LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, type.name());
            if (userId != null) {
                ps.setLong(3, userId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding category by name: " + name, e);
            throw new DataAccessException("Failed to query category by name.", e);
        }
        return Optional.empty();
    }

    @Override
    public Category save(Category category) {
        String sql = "INSERT INTO categories (user_id, name, type, is_system) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (category.getUserId() != null) {
                ps.setLong(1, category.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, category.getName().trim());
            ps.setString(3, category.getType().name());
            ps.setInt(4, category.isSystem() ? 1 : 0);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getLong(1));
                }
            }
            return category;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving category: " + category.getName(), e);
            throw new DataAccessException("Failed to save category.", e);
        }
    }

    @Override
    public boolean update(Category category) {
        String sql = "UPDATE categories SET name = ? WHERE id = ? AND is_system = 0 AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName().trim());
            ps.setLong(2, category.getId());
            ps.setLong(3, category.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating category id: " + category.getId(), e);
            throw new DataAccessException("Failed to update custom category.", e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long userId) {
        // Only custom user categories can be deleted
        String sql = "DELETE FROM categories WHERE id = ? AND user_id = ? AND is_system = 0";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting category id: " + id, e);
            throw new DataAccessException("Failed to delete custom category.", e);
        }
    }

    private Category mapRowToCategory(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long userId = rs.getObject("user_id") != null ? rs.getLong("user_id") : null;
        String name = rs.getString("name");
        TransactionType type = TransactionType.valueOf(rs.getString("type"));
        boolean isSystem = rs.getInt("is_system") == 1;

        return new Category(id, userId, name, type, isSystem);
    }
}
