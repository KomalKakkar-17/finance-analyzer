package com.financeanalyzer.service;

import com.financeanalyzer.dao.SavingsGoalDao;
import com.financeanalyzer.dao.jdbc.DatabaseManager;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.SavingsGoal;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.MoneyUtil;
import com.financeanalyzer.util.Validators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service orchestrating atomic ratio-based distribution of savings deposits across active goals.
 * Visibly demonstrates Unit 4 explicit ACID transaction with setAutoCommit(false), commit, and rollback.
 */
public class RatioAllocationService {

    private static final Logger LOGGER = AppLogger.getLogger(RatioAllocationService.class);

    private final SavingsGoalDao savingsGoalDao;

    public RatioAllocationService(SavingsGoalDao savingsGoalDao) {
        this.savingsGoalDao = Objects.requireNonNull(savingsGoalDao, "SavingsGoalDao must not be null.");
    }

    /**
     * Atomically distributes a savings deposit across active goals based on their individual allocation ratios.
     * Any unallocated percentage remains as unallocated savings.
     * The entire split is executed in a single atomic database transaction.
     *
     * @param userId user ID
     * @param depositAmount total deposit amount in rupees
     * @return AllocationResult containing itemized breakdown
     */
    public AllocationResult allocateDeposit(Long userId, BigDecimal depositAmount) {
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly positive.");
        }

        List<SavingsGoal> activeGoals = savingsGoalDao.findActiveGoals(userId);
        if (activeGoals.isEmpty()) {
            LOGGER.log(Level.INFO, "No active savings goals found for user {0}. Full deposit {1} is unallocated.",
                    new Object[]{userId, MoneyUtil.formatCurrency(depositAmount)});
            return new AllocationResult(depositAmount, BigDecimal.ZERO, depositAmount, Collections.emptyList());
        }

        List<GoalAllocationShare> shares = new ArrayList<>();
        BigDecimal totalAllocated = BigDecimal.ZERO;

        // Calculate distribution shares
        for (SavingsGoal goal : activeGoals) {
            if (goal.getAllocationRatio() > 0) {
                BigDecimal ratioFraction = BigDecimal.valueOf(goal.getAllocationRatio())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal goalShare = depositAmount.multiply(ratioFraction).setScale(2, RoundingMode.HALF_UP);

                shares.add(new GoalAllocationShare(goal.getId(), goal.getTitle(), goal.getAllocationRatio(), goalShare));
                totalAllocated = totalAllocated.add(goalShare);
            }
        }

        BigDecimal unallocatedRemainder = depositAmount.subtract(totalAllocated).max(BigDecimal.ZERO);

        // Execute atomic database transaction
        String updateSql = "UPDATE savings_goals SET saved_paise = saved_paise + ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Begin ACID Transaction
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                for (GoalAllocationShare share : shares) {
                    ps.setLong(1, MoneyUtil.toPaise(share.getAllocatedAmount()));
                    ps.setLong(2, share.getGoalId());
                    ps.setLong(3, userId);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit(); // Commit Transaction
                LOGGER.log(Level.INFO, "Atomically committed savings deposit allocation of {0} across {1} goals for user {2}.",
                        new Object[]{MoneyUtil.formatCurrency(depositAmount), shares.size(), userId});
            } catch (SQLException ex) {
                conn.rollback(); // Rollback on failure
                LOGGER.log(Level.SEVERE, "Rolled back atomic savings allocation transaction due to error: " + ex.getMessage(), ex);
                throw new DataAccessException("Failed to atomically allocate savings deposit.", ex);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error during savings allocation.", e);
            throw new DataAccessException("Database error during savings allocation.", e);
        }

        return new AllocationResult(depositAmount, totalAllocated, unallocatedRemainder, shares);
    }

    /**
     * DTO representing individual goal's allocated share.
     */
    public static class GoalAllocationShare {
        private final Long goalId;
        private final String goalTitle;
        private final int ratio;
        private final BigDecimal allocatedAmount;

        public GoalAllocationShare(Long goalId, String goalTitle, int ratio, BigDecimal allocatedAmount) {
            this.goalId = goalId;
            this.goalTitle = goalTitle;
            this.ratio = ratio;
            this.allocatedAmount = allocatedAmount;
        }

        public Long getGoalId() { return goalId; }
        public String getGoalTitle() { return goalTitle; }
        public int getRatio() { return ratio; }
        public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    }

    /**
     * Container summarizing complete deposit allocation results.
     */
    public static class AllocationResult {
        private final BigDecimal totalDeposit;
        private final BigDecimal totalAllocated;
        private final BigDecimal unallocatedRemainder;
        private final List<GoalAllocationShare> shares;

        public AllocationResult(BigDecimal totalDeposit, BigDecimal totalAllocated,
                                BigDecimal unallocatedRemainder, List<GoalAllocationShare> shares) {
            this.totalDeposit = totalDeposit;
            this.totalAllocated = totalAllocated;
            this.unallocatedRemainder = unallocatedRemainder;
            this.shares = shares != null ? shares : Collections.emptyList();
        }

        public BigDecimal getTotalDeposit() { return totalDeposit; }
        public BigDecimal getTotalAllocated() { return totalAllocated; }
        public BigDecimal getUnallocatedRemainder() { return unallocatedRemainder; }
        public List<GoalAllocationShare> getShares() { return shares; }
    }
}
