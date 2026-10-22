package com.financeanalyzer.rules;

import com.financeanalyzer.model.Transaction;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context object carrying historical and current financial metrics passed to Recommendation rules.
 * Visibly demonstrates Unit 1 OOP encapsulation.
 *
 * <p>The {@code fixedCategoryNames} set allows every heuristic rule to skip fixed/non-discretionary
 * categories (e.g. Rent, Utilities, EMI) so recommendations remain actionable.</p>
 */
public class AnalysisContext {

    private final Long userId;
    private final YearMonth currentMonth;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final Map<String, BigDecimal> currentMonthCategoryExpenses;
    private final Map<String, BigDecimal> lastMonthCategoryExpenses;
    private final List<Transaction> recentTransactions;

    /** Names of categories the user has marked as fixed (e.g. Rent, EMI). Never null. */
    private final Set<String> fixedCategoryNames;

    public AnalysisContext(Long userId,
                           YearMonth currentMonth,
                           BigDecimal totalIncome,
                           BigDecimal totalExpense,
                           Map<String, BigDecimal> currentMonthCategoryExpenses,
                           Map<String, BigDecimal> lastMonthCategoryExpenses,
                           List<Transaction> recentTransactions) {
        this(userId, currentMonth, totalIncome, totalExpense,
                currentMonthCategoryExpenses, lastMonthCategoryExpenses,
                recentTransactions, Collections.emptySet());
    }

    public AnalysisContext(Long userId,
                           YearMonth currentMonth,
                           BigDecimal totalIncome,
                           BigDecimal totalExpense,
                           Map<String, BigDecimal> currentMonthCategoryExpenses,
                           Map<String, BigDecimal> lastMonthCategoryExpenses,
                           List<Transaction> recentTransactions,
                           Set<String> fixedCategoryNames) {
        this.userId = userId;
        this.currentMonth = currentMonth;
        this.totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        this.totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
        this.currentMonthCategoryExpenses = currentMonthCategoryExpenses != null
                ? currentMonthCategoryExpenses : Collections.emptyMap();
        this.lastMonthCategoryExpenses = lastMonthCategoryExpenses != null
                ? lastMonthCategoryExpenses : Collections.emptyMap();
        this.recentTransactions = recentTransactions != null ? recentTransactions : Collections.emptyList();
        this.fixedCategoryNames = fixedCategoryNames != null
                ? Collections.unmodifiableSet(fixedCategoryNames) : Collections.emptySet();
    }

    public Long getUserId() { return userId; }
    public YearMonth getCurrentMonth() { return currentMonth; }
    public BigDecimal getTotalIncome() { return totalIncome; }
    public BigDecimal getTotalExpense() { return totalExpense; }
    public Map<String, BigDecimal> getCurrentMonthCategoryExpenses() { return currentMonthCategoryExpenses; }
    public Map<String, BigDecimal> getLastMonthCategoryExpenses() { return lastMonthCategoryExpenses; }
    public List<Transaction> getRecentTransactions() { return recentTransactions; }

    /**
     * Returns the set of category names the user has declared as fixed (non-discretionary).
     * Rules should call {@link #isFixed(String)} before generating a recommendation.
     */
    public Set<String> getFixedCategoryNames() { return fixedCategoryNames; }

    /**
     * Convenience helper: returns {@code true} if the given category name is in the fixed set.
     *
     * @param categoryName category name (case-insensitive)
     * @return true if fixed
     */
    public boolean isFixed(String categoryName) {
        if (categoryName == null) return false;
        return fixedCategoryNames.stream()
                .anyMatch(f -> f.equalsIgnoreCase(categoryName));
    }
}
