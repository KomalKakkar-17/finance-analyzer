package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.dao.UserDao;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.User;
import com.financeanalyzer.util.AppLogger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC implementation of UserDao using SQLite.
 * Demonstrates Unit 4 DAO pattern with PreparedStatements and private ResultSet mapping.
 */
public class JdbcUserDao implements UserDao {

    private static final Logger LOGGER = AppLogger.getLogger(JdbcUserDao.class);

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password_hash, recommendations_enabled, created_at FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by id: " + id, e);
            throw new DataAccessException("Failed to query user by id.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        String sql = "SELECT id, username, password_hash, recommendations_enabled, created_at FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by username: " + username, e);
            throw new DataAccessException("Failed to query user by username.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll(Long userId) {
        // Scoped query; returns current user or all if null
        String sql = userId != null ?
                "SELECT id, username, password_hash, recommendations_enabled, created_at FROM users WHERE id = ?" :
                "SELECT id, username, password_hash, recommendations_enabled, created_at FROM users";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setLong(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing users.", e);
            throw new DataAccessException("Failed to list users.", e);
        }
        return users;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (username, password_hash, recommendations_enabled, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername().trim());
                ps.setString(2, user.getPasswordHash());
                ps.setInt(3, user.isRecommendationsEnabled() ? 1 : 0);
                ps.setString(4, user.getCreatedAt() != null ? user.getCreatedAt().toString() : LocalDateTime.now().toString());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getLong(1));
                    }
                }
            }
            // Seed default fixed categories (Rent, Utilities) for every new user
            DatabaseManager.seedDefaultFixedCategories(conn, user.getId());
            conn.commit();
            return user;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting user: " + user.getUsername(), e);
            throw new DataAccessException("Failed to save user.", e);
        }
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET recommendations_enabled = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.isRecommendationsEnabled() ? 1 : 0);
            ps.setLong(2, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating user id: " + user.getId(), e);
            throw new DataAccessException("Failed to update user.", e);
        }
    }

    @Override
    public boolean updateRecommendationsSetting(Long userId, boolean recommendationsEnabled) {
        String sql = "UPDATE users SET recommendations_enabled = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recommendationsEnabled ? 1 : 0);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating recommendations toggle for user: " + userId, e);
            throw new DataAccessException("Failed to update user recommendations preference.", e);
        }
    }

    @Override
    public boolean deleteById(Long id, Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user id: " + id, e);
            throw new DataAccessException("Failed to delete user.", e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        boolean recs = rs.getInt("recommendations_enabled") == 1;
        String createdAtStr = rs.getString("created_at");
        LocalDateTime createdAt = (createdAtStr != null && !createdAtStr.isEmpty()) ?
                LocalDateTime.parse(createdAtStr) : LocalDateTime.now();

        return new User(id, username, passwordHash, recs, createdAt);
    }
}
