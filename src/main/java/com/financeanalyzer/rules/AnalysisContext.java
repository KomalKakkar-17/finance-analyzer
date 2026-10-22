package com.financeanalyzer.rules;

import com.financeanalyzer.model.Transaction;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Context object carrying historical and current financial metrics passed to Recommendation rules.
 * Visibly demonstrates Unit 1 OOP encapsulation.
 */
public class AnalysisContext {

    private final Long userId;
    private final YearMonth currentMonth;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final Map<String, BigDecimal> currentMonthCategoryExpenses;
    private final Map<String, BigDecimal> lastMonthCategoryExpenses;
    private final List<Transaction> recentTransactions;

    public AnalysisContext(Long userId,
                           YearMonth currentMonth,
                           BigDecimal totalIncome,
                           BigDecimal totalExpense,
                           Map<String, BigDecimal> currentMonthCategoryExpenses,
                           Map<String, BigDecimal> lastMonthCategoryExpenses,
                           List<Transaction> recentTransactions) {
        this.userId = userId;
        this.currentMonth = currentMonth;
        this.totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        this.totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
        this.currentMonthCategoryExpenses = currentMonthCategoryExpenses != null ? currentMonthCategoryExpenses : Collections.emptyMap();
        this.lastMonthCategoryExpenses = lastMonthCategoryExpenses != null ? lastMonthCategoryExpenses : Collections.emptyMap();
        this.recentTransactions = recentTransactions != null ? recentTransactions : Collections.emptyList();
    }

    public Long getUserId() { return userId; }
    public YearMonth getCurrentMonth() { return currentMonth; }
    public BigDecimal getTotalIncome() { return totalIncome; }
    public BigDecimal getTotalExpense() { return totalExpense; }
    public Map<String, BigDecimal> getCurrentMonthCategoryExpenses() { return currentMonthCategoryExpenses; }
    public Map<String, BigDecimal> getLastMonthCategoryExpenses() { return lastMonthCategoryExpenses; }
    public List<Transaction> getRecentTransactions() { return recentTransactions; }
}
