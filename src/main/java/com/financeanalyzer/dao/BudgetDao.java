package com.financeanalyzer.dao;

import com.financeanalyzer.model.Budget;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for monthly category budgets.
 */
public interface BudgetDao extends Dao<Budget, Long> {

    /**
     * Finds a budget for a specific category and month.
     *
     * @param userId user ID
     * @param categoryId category ID
     * @param yearMonth the budget month
     * @return Optional containing Budget if defined
     */
    Optional<Budget> findByCategoryAndMonth(Long userId, Long categoryId, YearMonth yearMonth);

    /**
     * Finds all budgets configured for a user in a specific month.
     *
     * @param userId user ID
     * @param yearMonth month
     * @return list of budgets
     */
    List<Budget> findAllForMonth(Long userId, YearMonth yearMonth);
}
