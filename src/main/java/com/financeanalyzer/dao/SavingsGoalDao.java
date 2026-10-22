package com.financeanalyzer.dao;

import com.financeanalyzer.model.SavingsGoal;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Access Object interface for savings goals.
 */
public interface SavingsGoalDao extends Dao<SavingsGoal, Long> {

    /**
     * Finds all currently active savings goals for a user.
     *
     * @param userId user ID
     * @return list of active savings goals
     */
    List<SavingsGoal> findActiveGoals(Long userId);

    /**
     * Atomically adds saved funds to a goal.
     *
     * @param goalId goal ID
     * @param userId user ID
     * @param amount amount to add
     * @return true if updated
     */
    boolean addFunds(Long goalId, Long userId, BigDecimal amount);

    /**
     * Updates allocation ratio for a specific goal.
     */
    boolean updateAllocationRatio(Long goalId, Long userId, int newRatio);

    /**
     * Toggles active status of a goal.
     */
    boolean updateActiveStatus(Long goalId, Long userId, boolean active);
}
