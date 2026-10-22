package com.financeanalyzer.service;

import com.financeanalyzer.dao.BudgetDao;
import com.financeanalyzer.dao.SavingsGoalDao;
import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.model.AnalyticsDTO;
import com.financeanalyzer.model.Budget;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.SavingsGoal;
import com.financeanalyzer.model.enums.AlertLevel;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Default implementation of InModalAnalyticsService.
 * Computes live in-modal metrics: category spend, projected utilization percentage,
 * threshold warnings, and savings goal impacts before saving an expense.
 */
public class DefaultInModalAnalyticsService implements InModalAnalyticsService {

    private static final Logger LOGGER = AppLogger.getLogger(DefaultInModalAnalyticsService.class);

    private final BudgetDao budgetDao;
    private final TransactionDao transactionDao;
    private final SavingsGoalDao savingsGoalDao;

    public DefaultInModalAnalyticsService(BudgetDao budgetDao,
                                          TransactionDao transactionDao,
                                          SavingsGoalDao savingsGoalDao) {
        this.budgetDao = Objects.requireNonNull(budgetDao, "BudgetDao must not be null.");
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null.");
        this.savingsGoalDao = Objects.requireNonNull(savingsGoalDao, "SavingsGoalDao must not be null.");
    }

    @Override
    public AnalyticsDTO calculatePreview(Long userId, Category category, BigDecimal proposedAmount, boolean isExpense) {
        if (userId == null) {
            return new AnalyticsDTO();
        }

        BigDecimal amount = (proposedAmount != null && proposedAmount.compareTo(BigDecimal.ZERO) > 0)
                ? proposedAmount
                : BigDecimal.ZERO;

        YearMonth currentMonth = YearMonth.now();

        // 1. Month-to-date category spend & budget
        BigDecimal categorySpent = BigDecimal.ZERO;
        BigDecimal budgetLimit = BigDecimal.ZERO;
        BigDecimal remainingBudget = BigDecimal.ZERO;
        BigDecimal projectedUtilisation = BigDecimal.ZERO;
        AlertLevel alertLevel = AlertLevel.NORMAL;
        boolean hasBudget = false;

        if (category != null && category.getId() != null) {
            categorySpent = transactionDao.getCategorySpendInMonth(userId, category.getId(), currentMonth);
            Optional<Budget> budgetOpt = budgetDao.findByCategoryAndMonth(userId, category.getId(), currentMonth);

            if (budgetOpt.isPresent()) {
                hasBudget = true;
                budgetLimit = budgetOpt.get().getLimit();
                remainingBudget = budgetLimit.subtract(categorySpent);
                if (remainingBudget.compareTo(BigDecimal.ZERO) < 0) {
                    remainingBudget = BigDecimal.ZERO;
                }

                if (isExpense && budgetLimit.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal totalProjected = categorySpent.add(amount);
                    projectedUtilisation = totalProjected.divide(budgetLimit, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP);
                    alertLevel = AlertLevel.fromUtilisation(projectedUtilisation);
                }
            }
        }

        // 2. Monthly Net Balance / Savings Impact
        BigDecimal totalIncome = transactionDao.getTotalIncomeInMonth(userId, currentMonth);
        BigDecimal totalExpense = transactionDao.getTotalExpenseInMonth(userId, currentMonth);
        BigDecimal savingsBefore = totalIncome.subtract(totalExpense);
        BigDecimal savingsAfter = isExpense ? savingsBefore.subtract(amount) : savingsBefore.add(amount);

        // 3. Goal Impact Previews
        List<AnalyticsDTO.GoalImpact> goalImpacts = new ArrayList<>();
        if (isExpense && amount.compareTo(BigDecimal.ZERO) > 0) {
            List<SavingsGoal> activeGoals = savingsGoalDao.findActiveGoals(userId);
            for (SavingsGoal goal : activeGoals) {
                if (goal.getAllocationRatio() > 0) {
                    BigDecimal reduction = amount.multiply(BigDecimal.valueOf(goal.getAllocationRatio()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    String msg = String.format("Reduces projected monthly goal savings by %s (%d%% share).",
                            MoneyUtil.formatCurrency(reduction), goal.getAllocationRatio());

                    goalImpacts.add(new AnalyticsDTO.GoalImpact(
                            goal.getTitle(),
                            goal.getTargetAmount(),
                            goal.getSavedAmount(),
                            goal.getAllocationRatio(),
                            reduction,
                            msg
                    ));
                }
            }
        }

        return new AnalyticsDTO(
                categorySpent,
                budgetLimit,
                remainingBudget,
                projectedUtilisation,
                alertLevel,
                hasBudget,
                amount,
                savingsBefore,
                savingsAfter,
                goalImpacts
        );
    }
}
