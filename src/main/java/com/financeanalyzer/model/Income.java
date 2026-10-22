package com.financeanalyzer.model;

import com.financeanalyzer.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Concrete entity representing an Income transaction.
 * Demonstrates Unit 1 inheritance and polymorphism.
 */
public class Income extends Transaction {

    private static final long serialVersionUID = 1L;

    public Income() {
        super();
    }

    public Income(Long id, Long userId, Long categoryId, String categoryName,
                  BigDecimal amount, LocalDate date, String paymentMode,
                  String notes, Long recurringRuleId, LocalDateTime createdAt) {
        super(id, userId, categoryId, categoryName, amount, date, paymentMode, notes, recurringRuleId, createdAt);
    }

    public Income(Long userId, Long categoryId, BigDecimal amount, LocalDate date, String paymentMode, String notes) {
        super(null, userId, categoryId, null, amount, date, paymentMode, notes, null, LocalDateTime.now());
    }

    @Override
    public TransactionType getType() {
        return TransactionType.INCOME;
    }

    /**
     * Incomes contribute positively to the balance.
     *
     * @return positive BigDecimal amount
     */
    @Override
    public BigDecimal getSignedAmount() {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
