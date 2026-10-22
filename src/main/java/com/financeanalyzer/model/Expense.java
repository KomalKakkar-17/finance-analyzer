package com.financeanalyzer.model;

import com.financeanalyzer.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Concrete entity representing an Expense transaction.
 * Demonstrates Unit 1 inheritance and polymorphism.
 */
public class Expense extends Transaction {

    private static final long serialVersionUID = 1L;

    public Expense() {
        super();
    }

    public Expense(Long id, Long userId, Long categoryId, String categoryName,
                   BigDecimal amount, LocalDate date, String paymentMode,
                   String notes, Long recurringRuleId, LocalDateTime createdAt) {
        super(id, userId, categoryId, categoryName, amount, date, paymentMode, notes, recurringRuleId, createdAt);
    }

    public Expense(Long userId, Long categoryId, BigDecimal amount, LocalDate date, String paymentMode, String notes) {
        super(null, userId, categoryId, null, amount, date, paymentMode, notes, null, LocalDateTime.now());
    }

    @Override
    public TransactionType getType() {
        return TransactionType.EXPENSE;
    }

    /**
     * Expenses reduce balance, returning negated amount.
     *
     * @return negative BigDecimal amount
     */
    @Override
    public BigDecimal getSignedAmount() {
        return amount != null ? amount.negate() : BigDecimal.ZERO;
    }
}
