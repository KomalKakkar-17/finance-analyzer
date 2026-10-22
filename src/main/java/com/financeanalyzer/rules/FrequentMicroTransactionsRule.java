package com.financeanalyzer.rules;

import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Rule 5: Triggers if there are five or more micro-transactions (expenses under ₹200) within the last 7 days.
 */
public class FrequentMicroTransactionsRule implements Recommendation {

    private static final BigDecimal MICRO_THRESHOLD = new BigDecimal("200.00");
    private static final int COUNT_THRESHOLD = 5;

    @Override
    public Optional<String> evaluate(AnalysisContext ctx) {
        List<Transaction> transactions = ctx.getRecentTransactions();
        if (transactions == null || transactions.isEmpty()) {
            return Optional.empty();
        }

        LocalDate weekAgo = LocalDate.now().minusDays(7);
        int microCount = 0;
        BigDecimal microSum = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE &&
                    t.getDate() != null && !t.getDate().isBefore(weekAgo)) {
                if (t.getAmount() != null && t.getAmount().compareTo(MICRO_THRESHOLD) < 0) {
                    microCount++;
                    microSum = microSum.add(t.getAmount());
                }
            }
        }

        if (microCount >= COUNT_THRESHOLD) {
            return Optional.of(String.format(
                    "Frequent Micro-Spends: You have made %d small purchases under \u20B9200 (totaling \u20B9%s) in the past week. Frequent micro-transactions often leak significant monthly capital.",
                    microCount, microSum.toPlainString()
            ));
        }

        return Optional.empty();
    }

    @Override
    public String getRuleName() {
        return "Frequent Micro-Transactions (>=5 under \u20B9200 in 7 days)";
    }
}
