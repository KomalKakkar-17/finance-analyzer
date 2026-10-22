package com.financeanalyzer.rules;

import com.financeanalyzer.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Rule 4: Triggers if the current net savings rate (net balance / income) is below 10%.
 */
public class LowSavingsRateRule implements Recommendation {

    private static final BigDecimal MIN_SAVINGS_RATE = new BigDecimal("0.10");

    @Override
    public Optional<String> evaluate(AnalysisContext ctx) {
        BigDecimal income = ctx.getTotalIncome();
        BigDecimal expense = ctx.getTotalExpense();

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal netSavings = income.subtract(expense);
        BigDecimal savingsRate = netSavings.divide(income, 4, RoundingMode.HALF_UP);

        if (savingsRate.compareTo(MIN_SAVINGS_RATE) < 0) {
            BigDecimal pct = savingsRate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            return Optional.of(String.format(
                    "Low Savings Rate: Your current net savings rate is %s%% (Target: >=10%%). Consider setting an automatic recurring savings deposit to build emergency buffers.",
                    pct
            ));
        }
        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "Low Savings Rate (<10%)";
    }
}
