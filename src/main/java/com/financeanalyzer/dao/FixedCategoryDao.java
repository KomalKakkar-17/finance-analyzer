package com.financeanalyzer.dao;

import java.util.Set;

/**
 * Data Access Object interface for the user_fixed_categories join table.
 * Manages which expense categories a user has marked as fixed (e.g. Rent, EMI).
 * Fixed categories are excluded from discretionary heuristic recommendations.
 *
 * <p>Strict layering rule: no SQL outside DAO implementations.</p>
 */
public interface FixedCategoryDao {

    /**
     * Returns the set of category names (not IDs) that are marked fixed for the given user.
     *
     * @param userId the user's primary key
     * @return immutable set of category name strings (may be empty, never null)
     */
    Set<String> getFixedCategoryNames(Long userId);

    /**
     * Returns the set of category IDs that are marked fixed for the given user.
     *
     * @param userId the user's primary key
     * @return set of category IDs (may be empty, never null)
     */
    Set<Long> getFixedCategoryIds(Long userId);

    /**
     * Marks an expense category as fixed for the given user.
     * Silently ignores duplicates (INSERT OR IGNORE).
     *
     * @param userId     the user's primary key
     * @param categoryId the category's primary key
     */
    void addFixedCategory(Long userId, Long categoryId);

    /**
     * Removes the fixed flag from a category for the given user.
     *
     * @param userId     the user's primary key
     * @param categoryId the category's primary key
     */
    void removeFixedCategory(Long userId, Long categoryId);

    /**
     * Returns {@code true} if the given category is marked fixed for the user.
     *
     * @param userId     the user's primary key
     * @param categoryId the category's primary key
     * @return true if fixed
     */
    boolean isFixed(Long userId, Long categoryId);
}
