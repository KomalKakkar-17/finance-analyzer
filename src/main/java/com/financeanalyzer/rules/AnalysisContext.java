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
    private final Set<Long> fixedCategoryIds;
    private final Map<Long, BigDecimal> currentMonthCategoryExpensesById;
    private final Map<Long, BigDecimal> lastMonthCategoryExpensesById;
    private final Map<Long, String> categoryNamesById;

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
        this(userId, currentMonth, totalIncome, totalExpense, currentMonthCategoryExpenses,
                lastMonthCategoryExpenses, recentTransactions, fixedCategoryNames,
                Collections.emptyMap(), Collections.emptyMap());
    }

    public AnalysisContext(Long userId,
                           YearMonth currentMonth,
                           BigDecimal totalIncome,
                           BigDecimal totalExpense,
                           Map<String, BigDecimal> currentMonthCategoryExpenses,
                           Map<String, BigDecimal> lastMonthCategoryExpenses,
                           List<Transaction> recentTransactions,
                           Map<Long, BigDecimal> currentMonthCategoryExpensesById,
                           Map<Long, BigDecimal> lastMonthCategoryExpensesById) {
        this(userId, currentMonth, totalIncome, totalExpense, currentMonthCategoryExpenses,
                lastMonthCategoryExpenses, recentTransactions, Collections.emptySet(),
                currentMonthCategoryExpensesById, lastMonthCategoryExpensesById);
    }

    private AnalysisContext(Long userId,
                            YearMonth currentMonth,
                            BigDecimal totalIncome,
                            BigDecimal totalExpense,
                            Map<String, BigDecimal> currentMonthCategoryExpenses,
                            Map<String, BigDecimal> lastMonthCategoryExpenses,
                            List<Transaction> recentTransactions,
                            Set<String> fixedCategoryNames,
                            Map<Long, BigDecimal> currentMonthCategoryExpensesById,
                            Map<Long, BigDecimal> lastMonthCategoryExpensesById) {
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
                ? Collections.unmodifiableSet(new java.util.HashSet<>(fixedCategoryNames)) : Collections.emptySet();
        this.fixedCategoryIds = Collections.emptySet();
        this.currentMonthCategoryExpensesById = currentMonthCategoryExpensesById != null
                ? Collections.unmodifiableMap(new java.util.HashMap<>(currentMonthCategoryExpensesById))
                : Collections.emptyMap();
        this.lastMonthCategoryExpensesById = lastMonthCategoryExpensesById != null
                ? Collections.unmodifiableMap(new java.util.HashMap<>(lastMonthCategoryExpensesById))
                : Collections.emptyMap();
        this.categoryNamesById = Collections.emptyMap();
    }

    private AnalysisContext(AnalysisContext source,
                            Set<Long> fixedCategoryIds,
                            Map<Long, BigDecimal> currentMonthCategoryExpensesById,
                            Map<Long, BigDecimal> lastMonthCategoryExpensesById,
                            Map<Long, String> categoryNamesById) {
        this.userId = source.userId;
        this.currentMonth = source.currentMonth;
        this.totalIncome = source.totalIncome;
        this.totalExpense = source.totalExpense;
        this.currentMonthCategoryExpenses = source.currentMonthCategoryExpenses;
        this.lastMonthCategoryExpenses = source.lastMonthCategoryExpenses;
        this.recentTransactions = source.recentTransactions;
        this.fixedCategoryNames = source.fixedCategoryNames;
        this.fixedCategoryIds = fixedCategoryIds != null
                ? Collections.unmodifiableSet(new java.util.HashSet<>(fixedCategoryIds))
                : Collections.emptySet();
        this.currentMonthCategoryExpensesById = currentMonthCategoryExpensesById != null
                ? Collections.unmodifiableMap(new java.util.HashMap<>(currentMonthCategoryExpensesById))
                : Collections.emptyMap();
        this.lastMonthCategoryExpensesById = lastMonthCategoryExpensesById != null
                ? Collections.unmodifiableMap(new java.util.HashMap<>(lastMonthCategoryExpensesById))
                : Collections.emptyMap();
        this.categoryNamesById = categoryNamesById != null
                ? Collections.unmodifiableMap(new java.util.HashMap<>(categoryNamesById))
                : Collections.emptyMap();
    }

    public AnalysisContext withFixedCategoryData(Set<Long> fixedCategoryIds,
                                                 Map<Long, BigDecimal> currentMonthCategoryExpensesById,
                                                 Map<Long, BigDecimal> lastMonthCategoryExpensesById,
                                                 Map<Long, String> categoryNamesById) {
        return new AnalysisContext(this, fixedCategoryIds, currentMonthCategoryExpensesById,
                lastMonthCategoryExpensesById, categoryNamesById);
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
    public Set<Long> getFixedCategoryIds() { return fixedCategoryIds; }
    public Map<Long, BigDecimal> getCurrentMonthCategoryExpensesById() {
        return currentMonthCategoryExpensesById;
    }
    public Map<Long, BigDecimal> getLastMonthCategoryExpensesById() {
        return lastMonthCategoryExpensesById;
    }
    public Map<Long, String> getCategoryNamesById() { return categoryNamesById; }

    public boolean isFixed(long categoryId) {
        return fixedCategoryIds.contains(categoryId);
    }

    /**
     * Convenience helper: returns {@code true} if the given category name is in the fixed set.
     *
     * @param categoryName category name (case-insensitive)
     * @return true if fixed
     */
    public boolean isFixed(String categoryName) {
        if (categoryName == null) return false;
        return fixedCategoryNames.stream().anyMatch(f -> f.equalsIgnoreCase(categoryName))
                || categoryNamesById.entrySet().stream()
                .anyMatch(entry -> entry.getValue().equalsIgnoreCase(categoryName)
                        && fixedCategoryIds.contains(entry.getKey()));
    }
}
