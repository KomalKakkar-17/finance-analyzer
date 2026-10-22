package com.financeanalyzer.rules;

import com.financeanalyzer.util.MoneyUtil;
import com.financeanalyzer.util.ConfigLoader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Rule 1: Triggers if any flexible expense category exceeds the configured share of flexible spending.
 *
 * <p>The denominator is total monthly expenses minus the amount spent in fixed categories, so fixed
 * obligations such as rent do not dilute the share of discretionary categories.</p>
 */
public class HighCategorySpendRule implements Recommendation {

    private static final BigDecimal THRESHOLD_PERCENT = new BigDecimal(
            ConfigLoader.getProperty("rule.category.share.threshold", "40"));

    @Override
    public Optional<String> evaluate(AnalysisContext ctx) {
        BigDecimal fixedExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> flexibleCategories = new java.util.LinkedHashMap<>();
        if (!ctx.getCurrentMonthCategoryExpensesById().isEmpty()) {
            for (Map.Entry<Long, BigDecimal> entry : ctx.getCurrentMonthCategoryExpensesById().entrySet()) {
                BigDecimal spend = entry.getValue();
                if (spend == null) {
                    continue;
                }
                if (ctx.isFixed(entry.getKey())) {
                    fixedExpense = fixedExpense.add(spend);
                } else if (spend.compareTo(BigDecimal.ZERO) > 0) {
                    String name = ctx.getCategoryNamesById().getOrDefault(entry.getKey(), entry.getKey().toString());
                    flexibleCategories.merge(name, spend, BigDecimal::add);
                }
            }
        } else {
            for (Map.Entry<String, BigDecimal> entry : ctx.getCurrentMonthCategoryExpenses().entrySet()) {
                if (ctx.isFixed(entry.getKey()) && entry.getValue() != null) {
                    fixedExpense = fixedExpense.add(entry.getValue());
                } else if (entry.getValue() != null) {
                    flexibleCategories.put(entry.getKey(), entry.getValue());
                }
            }
        }

        BigDecimal flexibleExpense = ctx.getTotalExpense().subtract(fixedExpense);
        if (flexibleExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        for (Map.Entry<String, BigDecimal> entry : flexibleCategories.entrySet()) {
            BigDecimal spend = entry.getValue();
            if (spend != null && spend.compareTo(BigDecimal.ZERO) > 0) {
                if (spend.multiply(BigDecimal.valueOf(100))
                        .compareTo(flexibleExpense.multiply(THRESHOLD_PERCENT)) > 0) {
                    BigDecimal share = spend.divide(flexibleExpense, 8, RoundingMode.HALF_UP);
                    BigDecimal pct = share.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
                    return Optional.of(String.format(
                            "High Concentration: '%s' constitutes %s%% (%s) of your flexible monthly spending. Consider diversifying or trimming discretionary purchases in this category.",
                            entry.getKey(), pct, MoneyUtil.formatCurrency(spend)
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "High Category Spend (>" + THRESHOLD_PERCENT.stripTrailingZeros().toPlainString() + "%)";
    }
}
