package com.financeanalyzer.dao;

import com.financeanalyzer.model.User;

import java.util.Optional;

/**
 * Data Access Object interface for User entities.
 */
public interface UserDao extends Dao<User, Long> {

    /**
     * Looks up a user account by exact username (case-insensitive).
     *
     * @param username the username string
     * @return Optional containing User if present
     */
    Optional<User> findByUsername(String username);

    /**
     * Updates user preferences such as recommendation toggle.
     *
     * @param userId the user ID
     * @param recommendationsEnabled whether smart recommendations are active
     * @return true if updated
     */
    boolean updateRecommendationsSetting(Long userId, boolean recommendationsEnabled);
}
