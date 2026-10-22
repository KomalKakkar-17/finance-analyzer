package com.financeanalyzer.exception;

import java.math.BigDecimal;

/**
 * Checked exception thrown when an expense transaction would breach a hard budget limit.
 */
public class BudgetExceededException extends AppException {

    private final String categoryName;
    private final BigDecimal limit;
    private final BigDecimal currentSpent;
    private final BigDecimal attemptAmount;

    public BudgetExceededException(String categoryName, BigDecimal limit, BigDecimal currentSpent, BigDecimal attemptAmount) {
        super(String.format("Budget exceeded for '%s': Current spent %s + Attempted %s exceeds limit of %s",
                categoryName, currentSpent, attemptAmount, limit));
        this.categoryName = categoryName;
        this.limit = limit;
        this.currentSpent = currentSpent;
        this.attemptAmount = attemptAmount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public BigDecimal getCurrentSpent() {
        return currentSpent;
    }

    public BigDecimal getAttemptAmount() {
        return attemptAmount;
    }
}
