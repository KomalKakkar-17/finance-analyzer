package com.financeanalyzer.rules;

import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Rule 3: Triggers if a category's current month spending is 25% higher than last month's spend in that category.
 */
public class CategorySpikeRule implements Recommendation {

    private static final BigDecimal SPIKE_THRESHOLD = new BigDecimal("0.25");

    @Override
    public Optional<String> evaluate(AnalysisContext ctx) {
        Map<String, BigDecimal> currentExpenses = ctx.getCurrentMonthCategoryExpenses();
        Map<String, BigDecimal> lastExpenses = ctx.getLastMonthCategoryExpenses();

        if (currentExpenses.isEmpty() || lastExpenses.isEmpty()) {
            return Optional.empty();
        }

        for (Map.Entry<String, BigDecimal> entry : currentExpenses.entrySet()) {
            String cat = entry.getKey();
            BigDecimal current = entry.getValue();
            BigDecimal last = lastExpenses.get(cat);

            // Compare only if there was meaningful spend last month (e.g. >= ₹500)
            if (current != null && last != null && last.compareTo(new BigDecimal("500")) >= 0) {
                BigDecimal diff = current.subtract(last);
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal increaseRatio = diff.divide(last, 4, RoundingMode.HALF_UP);
                    if (increaseRatio.compareTo(SPIKE_THRESHOLD) >= 0) {
                        BigDecimal pct = increaseRatio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
                        return Optional.of(String.format(
                                "Spending Spike: '%s' spending is up %s%% compared to last month (%s vs %s). Check for recurring bill changes or anomalous transactions.",
                                cat, pct, MoneyUtil.formatCurrency(current), MoneyUtil.formatCurrency(last)
                        ));
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "Category Spending Spike (>=25% MoM)";
    }
}
