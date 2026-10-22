package com.financeanalyzer.service;

import com.financeanalyzer.dao.SavingsGoalDao;
import com.financeanalyzer.exception.AllocationRatioException;
import com.financeanalyzer.model.SavingsGoal;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.MoneyUtil;
import com.financeanalyzer.util.Validators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing savings goals and allocation ratio constraints.
 * Implements FR-08 and FR-09.
 */
public class GoalService {

    private static final Logger LOGGER = AppLogger.getLogger(GoalService.class);

    private final SavingsGoalDao goalDao;

    public GoalService(SavingsGoalDao goalDao) {
        this.goalDao = Objects.requireNonNull(goalDao, "SavingsGoalDao must not be null.");
    }

    /**
     * Creates a new savings goal after validating constraints and allocation ratios.
     *
     * @throws AllocationRatioException if cumulative active ratio exceeds 100%
     */
    public SavingsGoal createGoal(Long userId, String title, BigDecimal targetAmount,
                                  LocalDate deadline, int ratio) throws AllocationRatioException {
        Validators.validateSavingsGoal(title, targetAmount, deadline, ratio);

        int currentTotal = getTotalActiveRatio(userId);
        if (currentTotal + ratio > 100) {
            throw new AllocationRatioException(
                    String.format("Allocation ratio %d%% causes active total (%d%%) to exceed 100%% cap.", ratio, currentTotal + ratio),
                    currentTotal + ratio
            );
        }

        SavingsGoal goal = new SavingsGoal(userId, title.trim(), targetAmount, deadline, ratio);
        SavingsGoal saved = goalDao.save(goal);
        LOGGER.log(Level.INFO, "Created savings goal '{0}' ({1}) with ratio {2}% for user {3}",
                new Object[]{title, MoneyUtil.formatCurrency(targetAmount), ratio, userId});
        return saved;
    }

    /**
     * Updates an existing savings goal.
     */
    public boolean updateGoal(SavingsGoal goal) throws AllocationRatioException {
        Objects.requireNonNull(goal, "Goal must not be null.");
        Validators.validateSavingsGoal(goal.getTitle(), goal.getTargetAmount(), goal.getDeadline(), goal.getAllocationRatio());

        if (goal.isActive()) {
            int otherGoalsTotal = 0;
            List<SavingsGoal> activeGoals = goalDao.findActiveGoals(goal.getUserId());
            for (SavingsGoal g : activeGoals) {
                if (!g.getId().equals(goal.getId())) {
                    otherGoalsTotal += g.getAllocationRatio();
                }
            }
            if (otherGoalsTotal + goal.getAllocationRatio() > 100) {
                throw new AllocationRatioException(
                        String.format("Updated ratio %d%% causes total (%d%%) to exceed 100%%.",
                                goal.getAllocationRatio(), otherGoalsTotal + goal.getAllocationRatio()),
                        otherGoalsTotal + goal.getAllocationRatio()
                );
            }
        }

        boolean updated = goalDao.update(goal);
        if (updated) {
            LOGGER.log(Level.INFO, "Updated savings goal ID {0} for user {1}", new Object[]{goal.getId(), goal.getUserId()});
        }
        return updated;
    }

    /**
     * Toggles the active status of a goal, enforcing ratio constraint if activated.
     */
    public boolean toggleGoalActive(Long goalId, Long userId) throws AllocationRatioException {
        SavingsGoal goal = goalDao.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Savings goal not found: " + goalId));

        boolean newActive = !goal.isActive();
        if (newActive) {
            int currentTotal = getTotalActiveRatio(userId);
            if (currentTotal + goal.getAllocationRatio() > 100) {
                throw new AllocationRatioException(
                        "Cannot activate goal: total allocation ratio would exceed 100%.",
                        currentTotal + goal.getAllocationRatio()
                );
            }
        }

        return goalDao.updateActiveStatus(goalId, userId, newActive);
    }

    public boolean deleteGoal(Long goalId, Long userId) {
        return goalDao.deleteById(goalId, userId);
    }

    public List<SavingsGoal> getActiveGoals(Long userId) {
        return goalDao.findActiveGoals(userId);
    }

    public List<SavingsGoal> getAllGoals(Long userId) {
        return goalDao.findAll(userId);
    }

    /**
     * Sums the allocation ratios across all active goals for a user.
     */
    public int getTotalActiveRatio(Long userId) {
        List<SavingsGoal> active = goalDao.findActiveGoals(userId);
        int sum = 0;
        for (SavingsGoal g : active) {
            sum += g.getAllocationRatio();
        }
        return sum;
    }
}
