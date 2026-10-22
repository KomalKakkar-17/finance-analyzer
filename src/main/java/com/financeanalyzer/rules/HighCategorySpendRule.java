package com.financeanalyzer.rules;

import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Rule 1: Triggers if any single expense category accounts for more than 40% of total monthly expenses.
 */
public class HighCategorySpendRule implements Recommendation {

    private static final BigDecimal THRESHOLD_RATIO = new BigDecimal("0.40");

    @Override
    public Optional<String> evaluate(AnalysisContext ctx) {
        BigDecimal totalExpense = ctx.getTotalExpense();
        if (totalExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        for (Map.Entry<String, BigDecimal> entry : ctx.getCurrentMonthCategoryExpenses().entrySet()) {
            BigDecimal spend = entry.getValue();
            if (spend != null && spend.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal share = spend.divide(totalExpense, 4, RoundingMode.HALF_UP);
                if (share.compareTo(THRESHOLD_RATIO) > 0) {
                    BigDecimal pct = share.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
                    return Optional.of(String.format(
                            "High Concentration: '%s' constitutes %s%% (%s) of your total monthly spending. Consider diversifying or trimming discretionary purchases in this category.",
                            entry.getKey(), pct, MoneyUtil.formatCurrency(spend)
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "High Category Spend (>40%)";
    }
}
