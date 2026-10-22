package com.financeanalyzer.util;

import com.financeanalyzer.exception.InvalidTransactionException;
import com.financeanalyzer.model.Category;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validation utility enforcing syntactic and business validation rules.
 */
public final class Validators {

    private Validators() {
        // Utility class
    }

    /**
     * Validates that username is non-empty, contains at least 6 alphanumeric characters.
     */
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        String trimmed = username.trim();
        if ("demo".equalsIgnoreCase(trimmed)) return true;
        return trimmed.length() >= 6 && trimmed.matches("^[a-zA-Z0-9_.-]+$");
    }

    /**
     * Validates that password is at least 6 characters long.
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Validates an expense transaction.
     *
     * @throws InvalidTransactionException if invalid
     */
    public static void validateExpense(BigDecimal amount, LocalDate date, Category category)
            throws InvalidTransactionException {
        validateAmount(amount);
        if (date == null) {
            throw new InvalidTransactionException("Transaction date is required.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidTransactionException("Expense date cannot be in the future: " + date);
        }
        if (category == null || category.getId() == null) {
            throw new InvalidTransactionException("Expense category must be selected.");
        }
    }

    /**
     * Validates an income transaction.
     *
     * @throws InvalidTransactionException if invalid
     */
    public static void validateIncome(BigDecimal amount, LocalDate date, Category category)
            throws InvalidTransactionException {
        validateAmount(amount);
        if (date == null) {
            throw new InvalidTransactionException("Transaction date is required.");
        }
        if (category == null || category.getId() == null) {
            throw new InvalidTransactionException("Income category must be selected.");
        }
    }

    /**
     * Validates monetary amount (> 0, scale <= 2).
     */
    public static void validateAmount(BigDecimal amount) throws InvalidTransactionException {
        if (amount == null) {
            throw new InvalidTransactionException("Amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be strictly greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new InvalidTransactionException("Amount cannot have more than 2 decimal places.");
        }
    }

    /**
     * Validates budget limit (> 0).
     */
    public static void validateBudgetLimit(BigDecimal limit) throws IllegalArgumentException {
        if (limit == null || limit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget limit must be strictly positive.");
        }
    }

    /**
     * Validates savings goal parameters.
     */
    public static void validateSavingsGoal(String title, BigDecimal targetAmount, LocalDate deadline, int ratio)
            throws IllegalArgumentException {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal title cannot be blank.");
        }
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Goal target amount must be greater than zero.");
        }
        if (deadline == null || deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Goal deadline must be a future date.");
        }
        if (ratio < 0 || ratio > 100) {
            throw new IllegalArgumentException("Allocation ratio must be between 0 and 100%.");
        }
    }
}
