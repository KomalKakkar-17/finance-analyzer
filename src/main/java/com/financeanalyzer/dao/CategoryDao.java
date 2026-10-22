package com.financeanalyzer.dao;

import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.enums.TransactionType;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Category entities.
 */
public interface CategoryDao extends Dao<Category, Long> {

    /**
     * Retrieves all categories accessible to a user (system categories + custom user categories).
     *
     * @param userId user ID
     * @param type transaction type filter (INCOME, EXPENSE, or null for all)
     * @return list of categories
     */
    List<Category> findCategoriesForUser(Long userId, TransactionType type);

    /**
     * Finds a category by case-insensitive name and type for a user or system.
     * Demonstrates Unit 2 Optional finders requirement.
     *
     * @param userId user ID
     * @param name category name
     * @param type transaction type
     * @return Optional containing Category
     */
    Optional<Category> findByName(Long userId, String name, TransactionType type);
}
