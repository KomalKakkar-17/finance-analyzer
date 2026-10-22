package com.financeanalyzer.rules;

import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Rule 2: Triggers if total monthly expense exceeds 90% of total monthly income.
 */
public class HighExpenseToIncomeRule implements Recommendation {

    private static final BigDecimal THRESHOLD_RATIO = new BigDecimal("0.90");

    @Override
    public Optional<String> evaluate(AnalysisContext ctx) {
        BigDecimal income = ctx.getTotalIncome();
        BigDecimal expense = ctx.getTotalExpense();

        if (income.compareTo(BigDecimal.ZERO) <= 0 || expense.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal ratio = expense.divide(income, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(THRESHOLD_RATIO) > 0) {
            BigDecimal pct = ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            return Optional.of(String.format(
                    "Elevated Burn Rate: You have spent %s%% of your monthly income (%s of %s). Tighten non-essential expenses to avoid dipping into reserves.",
                    pct, MoneyUtil.formatCurrency(expense), MoneyUtil.formatCurrency(income)
            ));
        }
        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "High Expense-to-Income (>90%)";
    }
}
