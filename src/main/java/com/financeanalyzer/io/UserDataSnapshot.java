package com.financeanalyzer.io;

import com.financeanalyzer.model.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Serializable data container capturing an entire user account snapshot.
 * Visibly demonstrates Unit 3 Object Serialization (ObjectOutputStream / ObjectInputStream)
 * with a declared serialVersionUID.
 */
public class UserDataSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final User user;
    private final List<Category> customCategories;
    private final List<Transaction> transactions;
    private final List<Budget> budgets;
    private final List<SavingsGoal> savingsGoals;
    private final List<RecurringTransaction> recurringRules;
    private final LocalDateTime snapshotTimestamp;

    public UserDataSnapshot(User user,
                            List<Category> customCategories,
                            List<Transaction> transactions,
                            List<Budget> budgets,
                            List<SavingsGoal> savingsGoals,
                            List<RecurringTransaction> recurringRules) {
        this.user = user;
        this.customCategories = customCategories != null ? List.copyOf(customCategories) : Collections.emptyList();
        this.transactions = transactions != null ? List.copyOf(transactions) : Collections.emptyList();
        this.budgets = budgets != null ? List.copyOf(budgets) : Collections.emptyList();
        this.savingsGoals = savingsGoals != null ? List.copyOf(savingsGoals) : Collections.emptyList();
        this.recurringRules = recurringRules != null ? List.copyOf(recurringRules) : Collections.emptyList();
        this.snapshotTimestamp = LocalDateTime.now();
    }

    public User getUser() { return user; }
    public List<Category> getCustomCategories() { return customCategories; }
    public List<Transaction> getTransactions() { return transactions; }
    public List<Budget> getBudgets() { return budgets; }
    public List<SavingsGoal> getSavingsGoals() { return savingsGoals; }
    public List<RecurringTransaction> getRecurringRules() { return recurringRules; }
    public LocalDateTime getSnapshotTimestamp() { return snapshotTimestamp; }
}
