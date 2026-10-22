package com.financeanalyzer.service;

import com.financeanalyzer.dao.BudgetDao;
import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.dao.SavingsGoalDao;
import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.model.Budget;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.SavingsGoal;
import com.financeanalyzer.model.User;
import com.financeanalyzer.rules.AnalysisContext;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service generating formatted monthly financial audit and performance reports.
 * Implements FR-14.
 * Demonstrates Unit 2 TreeMap collection for alphabetically/month-ordered summaries.
 */
public class ReportService {

    private static final Logger LOGGER = AppLogger.getLogger(ReportService.class);

    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final SavingsGoalDao savingsGoalDao;
    private final HeuristicEngine heuristicEngine;
    private final CategoryService categoryService;

    public ReportService(TransactionDao transactionDao,
                         BudgetDao budgetDao,
                         SavingsGoalDao savingsGoalDao,
                         HeuristicEngine heuristicEngine,
                         CategoryService categoryService) {
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null.");
        this.budgetDao = Objects.requireNonNull(budgetDao, "BudgetDao must not be null.");
        this.savingsGoalDao = Objects.requireNonNull(savingsGoalDao, "SavingsGoalDao must not be null.");
        this.heuristicEngine = Objects.requireNonNull(heuristicEngine, "HeuristicEngine must not be null.");
        this.categoryService = Objects.requireNonNull(categoryService, "CategoryService must not be null.");
    }

    /**
     * Generates a comprehensive plain-text monthly financial statement.
     *
     * @param user authenticated user
     * @param yearMonth report month
     * @return formatted report text
     */
    public String generateMonthlyReportText(User user, YearMonth yearMonth) {
        Long userId = user.getId();
        BigDecimal totalIncome = transactionDao.getTotalIncomeInMonth(userId, yearMonth);
        BigDecimal totalExpense = transactionDao.getTotalExpenseInMonth(userId, yearMonth);
        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        // Demonstrates Unit 2 Collections: TreeMap for sorted category breakdown
        Map<String, BigDecimal> rawBreakdown = transactionDao.getCategoryExpenseBreakdown(userId, yearMonth);
        TreeMap<String, BigDecimal> sortedBreakdown = new TreeMap<>(rawBreakdown);

        List<Budget> budgets = budgetDao.findAllForMonth(userId, yearMonth);
        List<SavingsGoal> activeGoals = savingsGoalDao.findActiveGoals(userId);

        // Run Heuristics for the report month
        YearMonth lastMonth = yearMonth.minusMonths(1);
        Map<String, BigDecimal> lastBreakdown = transactionDao.getCategoryExpenseBreakdown(userId, lastMonth);
        Map<Long, BigDecimal> currentBreakdownById =
                transactionDao.getCategoryExpenseBreakdownById(userId, yearMonth);
        Map<Long, BigDecimal> lastBreakdownById =
                transactionDao.getCategoryExpenseBreakdownById(userId, lastMonth);
        AnalysisContext ctx = new AnalysisContext(
                userId, yearMonth, totalIncome, totalExpense,
                sortedBreakdown, lastBreakdown, transactionDao.findRecent(userId, 50),
                currentBreakdownById, lastBreakdownById
        );
        List<String> recommendations = heuristicEngine.generateRecommendations(user, ctx);

        StringBuilder sb = new StringBuilder();
        String border = "================================================================================";
        String line = "--------------------------------------------------------------------------------";

        sb.append(border).append(System.lineSeparator());
        sb.append(String.format("         ENHANCED PERSONAL FINANCE ANALYZER - MONTHLY REPORT%n"));
        sb.append(String.format("         User: %s  |  Report Period: %s%n", user.getUsername(), DateUtil.formatDisplayMonth(yearMonth)));
        sb.append(String.format("         Generated At: %s%n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        sb.append(border).append(System.lineSeparator()).append(System.lineSeparator());

        // 1. EXECUTIVE FINANCIAL SUMMARY
        sb.append("1. EXECUTIVE SUMMARY").append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(String.format("  Total Income:         %20s%n", MoneyUtil.formatCurrency(totalIncome)));
        sb.append(String.format("  Total Expense:        %20s%n", MoneyUtil.formatCurrency(totalExpense)));
        sb.append(String.format("  Net Savings/Surplus:  %20s%n", MoneyUtil.formatCurrency(netSavings)));
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savingsRate = netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            sb.append(String.format("  Net Savings Rate:     %19.1f%%%n", savingsRate.doubleValue()));
        }
        sb.append(System.lineSeparator());

        // 2. CATEGORY-WISE EXPENSE BREAKDOWN (Using TreeMap)
        sb.append("2. CATEGORY-WISE EXPENSES").append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(String.format("  %-28s %18s %15s%n", "Category", "Amount Spent", "Share of Total"));
        sb.append(String.format("  %-28s %18s %15s%n", "--------", "------------", "--------------"));

        if (sortedBreakdown.isEmpty()) {
            sb.append("  No expenses recorded for this month.").append(System.lineSeparator());
        } else {
            for (Map.Entry<String, BigDecimal> entry : sortedBreakdown.entrySet()) {
                BigDecimal sharePct = BigDecimal.ZERO;
                if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                    sharePct = entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                }
                sb.append(String.format("  %-28s %18s %14.1f%%%n",
                        entry.getKey(), MoneyUtil.formatCurrency(entry.getValue()), sharePct.doubleValue()));
            }
        }
        sb.append(System.lineSeparator());

        // 3. FIXED VS FLEXIBLE EXPENSES
        Set<Long> fixedCategoryIds = categoryService.findFixedCategoryIds(userId);
        BigDecimal fixedTotal = BigDecimal.ZERO;
        BigDecimal flexibleTotal = BigDecimal.ZERO;
        List<Category> expenseCategories = categoryService.findExpenseCategories(userId);
        for (Category category : expenseCategories) {
            BigDecimal amount = currentBreakdownById.getOrDefault(category.getId(), BigDecimal.ZERO);
            if (fixedCategoryIds.contains(category.getId())) {
                fixedTotal = fixedTotal.add(amount);
            } else {
                flexibleTotal = flexibleTotal.add(amount);
            }
        }

        sb.append("3. FIXED VS FLEXIBLE EXPENSES").append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(String.format("  Fixed Total:             %20s%n", MoneyUtil.formatCurrency(fixedTotal)));
        sb.append(String.format("  Flexible Total:          %20s%n", MoneyUtil.formatCurrency(flexibleTotal)));
        sb.append(String.format("  %-28s %18s %12s%n", "Category", "Amount Spent", "Classification"));
        sb.append(String.format("  %-28s %18s %12s%n", "--------", "------------", "--------------"));
        if (expenseCategories.isEmpty()) {
            sb.append("  No expense categories available.").append(System.lineSeparator());
        } else {
            for (Category category : expenseCategories) {
                boolean fixed = fixedCategoryIds.contains(category.getId());
                BigDecimal amount = currentBreakdownById.getOrDefault(category.getId(), BigDecimal.ZERO);
                sb.append(String.format("  %-28s %18s %12s%n",
                        category.getName(), MoneyUtil.formatCurrency(amount), fixed ? "Fixed" : "Flexible"));
            }
        }
        sb.append(System.lineSeparator());

        // 4. BUDGET PERFORMANCE VS ACTUAL
        sb.append("4. BUDGET PERFORMANCE VS ACTUAL").append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(String.format("  %-22s %14s %14s %14s %10s%n", "Category", "Budget Limit", "Actual Spend", "Remaining", "Util %"));
        sb.append(String.format("  %-22s %14s %14s %14s %10s%n", "--------", "------------", "------------", "---------", "------"));

        if (budgets.isEmpty()) {
            sb.append("  No category budgets configured for this month.").append(System.lineSeparator());
        } else {
            for (Budget b : budgets) {
                BigDecimal spent = transactionDao.getCategorySpendInMonth(userId, b.getCategoryId(), yearMonth);
                BigDecimal rem = b.getLimit().subtract(spent);
                BigDecimal util = b.getLimit().compareTo(BigDecimal.ZERO) > 0
                        ? spent.divide(b.getLimit(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                String status = util.doubleValue() >= 100.0 ? " [EXCEEDED]" : (util.doubleValue() >= 80.0 ? " [WARN]" : "");
                sb.append(String.format("  %-22s %14s %14s %14s %9.1f%%%s%n",
                        b.getCategoryName(),
                        MoneyUtil.formatCurrency(b.getLimit()),
                        MoneyUtil.formatCurrency(spent),
                        MoneyUtil.formatCurrency(rem.max(BigDecimal.ZERO)),
                        util.doubleValue(),
                        status));
            }
        }
        sb.append(System.lineSeparator());

        // 5. SAVINGS GOALS STATUS
        sb.append("5. SAVINGS GOALS STATUS").append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(String.format("  %-24s %14s %14s %12s %8s%n", "Goal Title", "Target", "Saved So Far", "Deadline", "Progress"));
        sb.append(String.format("  %-24s %14s %14s %12s %8s%n", "----------", "------", "------------", "--------", "--------"));

        if (activeGoals.isEmpty()) {
            sb.append("  No active savings goals found.").append(System.lineSeparator());
        } else {
            for (SavingsGoal g : activeGoals) {
                sb.append(String.format("  %-24s %14s %14s %12s %7.1f%%%n",
                        g.getTitle(),
                        MoneyUtil.formatCurrency(g.getTargetAmount()),
                        MoneyUtil.formatCurrency(g.getSavedAmount()),
                        DateUtil.formatDisplayDate(g.getDeadline()),
                        g.getProgressPercentage()));
            }
        }
        sb.append(System.lineSeparator());

        // 6. SMART HEURISTIC RECOMMENDATIONS (FR-13)
        sb.append("6. SMART FINANCIAL RECOMMENDATIONS").append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        if (!user.isRecommendationsEnabled()) {
            sb.append("  (Smart recommendations are currently disabled in user settings.)").append(System.lineSeparator());
        } else if (recommendations.isEmpty()) {
            sb.append("  All metrics healthy! No warning flags or budget imbalances detected.").append(System.lineSeparator());
        } else {
            for (int i = 0; i < recommendations.size(); i++) {
                sb.append(String.format("  [%d] %s%n", i + 1, recommendations.get(i)));
            }
        }

        sb.append(System.lineSeparator()).append(border).append(System.lineSeparator());
        sb.append("                   END OF FINANCIAL REPORT").append(System.lineSeparator());
        sb.append(border).append(System.lineSeparator());

        return sb.toString();
    }
}
