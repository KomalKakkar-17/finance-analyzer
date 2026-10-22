package com.financeanalyzer.service;

import com.financeanalyzer.dao.BudgetDao;
import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.exception.BudgetExceededException;
import com.financeanalyzer.model.Budget;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.enums.AlertLevel;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.MoneyUtil;
import com.financeanalyzer.util.Validators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service orchestrating monthly category budgets and threshold alerts.
 * Implements FR-06 and FR-07.
 * Calculates utilization percentages and fires Unit 3 WARNING logs upon threshold breaches.
 */
public class BudgetService {

    private static final Logger LOGGER = AppLogger.getLogger(BudgetService.class);

    private final BudgetDao budgetDao;
    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;

    public BudgetService(BudgetDao budgetDao, TransactionDao transactionDao, CategoryDao categoryDao) {
        this.budgetDao = Objects.requireNonNull(budgetDao, "BudgetDao must not be null.");
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null.");
        this.categoryDao = Objects.requireNonNull(categoryDao, "CategoryDao must not be null.");
    }

    /**
     * Sets or updates a monthly budget limit for a category.
     */
    public Budget setBudget(Long userId, Long categoryId, YearMonth yearMonth, BigDecimal limit) {
        Validators.validateBudgetLimit(limit);
        Category category = categoryDao.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category does not exist: " + categoryId));

        Budget budget = new Budget(userId, categoryId, yearMonth, limit);
        budget.setCategoryName(category.getName());
        Budget saved = budgetDao.save(budget);

        LOGGER.log(Level.INFO, "Budget set for user {0}, category {1}, month {2}: {3}",
                new Object[]{userId, category.getName(), yearMonth, MoneyUtil.formatCurrency(limit)});
        return saved;
    }

    /**
     * Deletes a budget limit.
     */
    public boolean deleteBudget(Long budgetId, Long userId) {
        boolean deleted = budgetDao.deleteById(budgetId, userId);
        if (deleted) {
            LOGGER.log(Level.INFO, "Deleted budget ID {0} for user {1}", new Object[]{budgetId, userId});
        }
        return deleted;
    }

    /**
     * Finds all budgets for a given month.
     */
    public List<Budget> getBudgetsForMonth(Long userId, YearMonth yearMonth) {
        return budgetDao.findAllForMonth(userId, yearMonth);
    }

    /**
     * Finds budget for a specific category in a month.
     */
    public Optional<Budget> getBudgetByCategory(Long userId, Long categoryId, YearMonth yearMonth) {
        return budgetDao.findByCategoryAndMonth(userId, categoryId, yearMonth);
    }

    /**
     * Calculates projected utilization percentage for a category.
     * Utilisation % = (category month-to-date expense + additionalSpend) / budget limit x 100.
     */
    public BigDecimal calculateUtilisation(Long userId, Long categoryId, YearMonth yearMonth, BigDecimal additionalSpend) {
        Optional<Budget> budgetOpt = budgetDao.findByCategoryAndMonth(userId, categoryId, yearMonth);
        if (budgetOpt.isEmpty() || budgetOpt.get().getLimit().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal limit = budgetOpt.get().getLimit();
        BigDecimal currentSpent = transactionDao.getCategorySpendInMonth(userId, categoryId, yearMonth);
        BigDecimal totalProjected = currentSpent.add(additionalSpend != null ? additionalSpend : BigDecimal.ZERO);

        return totalProjected.divide(limit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks whether an expense breaches warning or critical thresholds.
     * Logs Unit 3 WARNING upon threshold breaches.
     */
    public AlertLevel checkBudgetAlert(Long userId, Long categoryId, YearMonth yearMonth, BigDecimal attemptSpend)
            throws BudgetExceededException {
        Optional<Budget> budgetOpt = budgetDao.findByCategoryAndMonth(userId, categoryId, yearMonth);
        if (budgetOpt.isEmpty()) {
            return AlertLevel.NORMAL;
        }

        Budget budget = budgetOpt.get();
        BigDecimal currentSpent = transactionDao.getCategorySpendInMonth(userId, categoryId, yearMonth);
        BigDecimal total = currentSpent.add(attemptSpend != null ? attemptSpend : BigDecimal.ZERO);
        BigDecimal util = calculateUtilisation(userId, categoryId, yearMonth, attemptSpend);
        AlertLevel level = AlertLevel.fromUtilisation(util);

        if (level == AlertLevel.EXCEEDED) {
            LOGGER.log(Level.WARNING, "BUDGET EXCEEDED! User {0}, Category {1}: limit={2}, projected total={3}",
                    new Object[]{userId, budget.getCategoryName(), MoneyUtil.formatCurrency(budget.getLimit()), MoneyUtil.formatCurrency(total)});
        } else if (level == AlertLevel.WARNING) {
            LOGGER.log(Level.WARNING, "BUDGET WARNING! User {0}, Category {1}: spend reached {2}% of limit ({3})",
                    new Object[]{userId, budget.getCategoryName(), util, MoneyUtil.formatCurrency(budget.getLimit())});
        }

        return level;
    }
}
