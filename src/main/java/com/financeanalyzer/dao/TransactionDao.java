package com.financeanalyzer.dao;

import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object interface for financial Transactions.
 */
public interface TransactionDao extends Dao<Transaction, Long> {

    /**
     * Executes a multi-criteria filtered search query over transactions.
     * Demonstrates Unit 4 requirement for filtered SELECT (date range, category, type, keyword, amount range).
     *
     * @param userId user ID
     * @param fromDate starting date (inclusive, optional)
     * @param toDate ending date (inclusive, optional)
     * @param categoryId category filter (optional)
     * @param type transaction type (optional)
     * @param minAmount minimum amount (optional)
     * @param maxAmount maximum amount (optional)
     * @param keyword search keyword matching notes (optional)
     * @return filtered list of transactions
     */
    List<Transaction> findFiltered(Long userId, LocalDate fromDate, LocalDate toDate,
                                  Long categoryId, TransactionType type,
                                  BigDecimal minAmount, BigDecimal maxAmount, String keyword);

    /**
     * Sums all expenses for a specific category within a given month.
     *
     * @param userId user ID
     * @param categoryId category ID
     * @param yearMonth the month
     * @return month-to-date spent BigDecimal
     */
    BigDecimal getCategorySpendInMonth(Long userId, Long categoryId, YearMonth yearMonth);

    /**
     * Sums all expenses for all categories in a month.
     */
    BigDecimal getTotalExpenseInMonth(Long userId, YearMonth yearMonth);

    /**
     * Sums all incomes for a user in a month.
     */
    BigDecimal getTotalIncomeInMonth(Long userId, YearMonth yearMonth);

    /**
     * Retrieves category-wise expense breakdown for a given month.
     * Used for charts and reporting (demonstrates Unit 2 Map collection).
     */
    Map<String, BigDecimal> getCategoryExpenseBreakdown(Long userId, YearMonth yearMonth);

    /**
     * Retrieves category-wise expense totals keyed by category ID for fixed-category analysis.
     */
    Map<Long, BigDecimal> getCategoryExpenseBreakdownById(Long userId, YearMonth yearMonth);

    /**
     * Retrieves the latest N transactions for the recent activity queue.
     * Demonstrates Unit 2 Collections (LinkedList).
     */
    List<Transaction> findRecent(Long userId, int limit);
}
