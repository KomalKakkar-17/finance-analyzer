package com.financeanalyzer.io;

import com.financeanalyzer.dao.*;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.*;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.util.AppLogger;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages full byte-stream object serialization snapshots and restores.
 * Demonstrates Unit 3 byte streams (ObjectOutputStream / ObjectInputStream) and serialVersionUID.
 */
public class BackupManager {

    private static final Logger LOGGER = AppLogger.getLogger(BackupManager.class);

    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final SavingsGoalDao savingsGoalDao;
    private final RecurringTransactionDao recurringDao;

    public BackupManager(CategoryDao categoryDao,
                         TransactionDao transactionDao,
                         BudgetDao budgetDao,
                         SavingsGoalDao savingsGoalDao,
                         RecurringTransactionDao recurringDao) {
        this.categoryDao = Objects.requireNonNull(categoryDao, "CategoryDao must not be null.");
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null.");
        this.budgetDao = Objects.requireNonNull(budgetDao, "BudgetDao must not be null.");
        this.savingsGoalDao = Objects.requireNonNull(savingsGoalDao, "SavingsGoalDao must not be null.");
        this.recurringDao = Objects.requireNonNull(recurringDao, "RecurringTransactionDao must not be null.");
    }

    /**
     * Serializes user data into a .dat snapshot file via ObjectOutputStream.
     */
    public void createBackup(User user, File destinationFile) throws IOException {
        Objects.requireNonNull(user, "User cannot be null.");
        Objects.requireNonNull(destinationFile, "Destination file cannot be null.");

        Long userId = user.getId();
        LOGGER.log(Level.INFO, "Initiating full binary data backup for user {0} (ID: {1})",
                new Object[]{user.getUsername(), userId});

        List<Category> customCats = categoryDao.findAll(userId).stream()
                .filter(c -> !c.isSystem())
                .toList();
        List<Transaction> transactions = transactionDao.findAll(userId);
        List<Budget> budgets = budgetDao.findAll(userId);
        List<SavingsGoal> goals = savingsGoalDao.findAll(userId);
        List<RecurringTransaction> rules = recurringDao.findAll(userId);

        UserDataSnapshot snapshot = new UserDataSnapshot(user, customCats, transactions, budgets, goals, rules);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(destinationFile)))) {
            oos.writeObject(snapshot);
            oos.flush();
            LOGGER.log(Level.INFO, "Successfully serialized {0} transactions and snapshot data to {1}",
                    new Object[]{transactions.size(), destinationFile.getAbsolutePath()});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize backup file: " + destinationFile.getName(), e);
            throw e;
        }
    }

    /**
     * Restores user financial data from a .dat snapshot file via ObjectInputStream.
     */
    public UserDataSnapshot restoreBackup(User currentUser, File sourceFile) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(currentUser, "Current user cannot be null.");
        Objects.requireNonNull(sourceFile, "Source file cannot be null.");

        LOGGER.log(Level.INFO, "Restoring binary data snapshot from {0} for user {1}",
                new Object[]{sourceFile.getAbsolutePath(), currentUser.getUsername()});

        UserDataSnapshot snapshot;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(sourceFile)))) {
            Object obj = ois.readObject();
            if (!(obj instanceof UserDataSnapshot s)) {
                throw new ClassNotFoundException("Invalid backup file: not a UserDataSnapshot object.");
            }
            snapshot = s;
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize backup file: " + sourceFile.getName(), e);
            throw e;
        }

        Long targetUserId = currentUser.getId();

        // 1. Restore Custom Categories
        for (Category cat : snapshot.getCustomCategories()) {
            if (categoryDao.findByName(targetUserId, cat.getName(), cat.getType()).isEmpty()) {
                Category newCat = new Category(null, targetUserId, cat.getName(), cat.getType(), false);
                categoryDao.save(newCat);
            }
        }

        // 2. Restore Budgets
        for (Budget b : snapshot.getBudgets()) {
            Category matchCat = categoryDao.findByName(targetUserId, b.getCategoryName(), TransactionType.EXPENSE)
                    .orElse(null);
            if (matchCat != null) {
                Budget newBudget = new Budget(null, targetUserId, matchCat.getId(), matchCat.getName(), b.getYearMonth(), b.getLimit());
                budgetDao.save(newBudget);
            }
        }

        // 3. Restore Savings Goals
        for (SavingsGoal g : snapshot.getSavingsGoals()) {
            SavingsGoal newGoal = new SavingsGoal(
                    null, targetUserId, g.getTitle(), g.getTargetAmount(), g.getSavedAmount(),
                    g.getDeadline(), g.getAllocationRatio(), g.isActive()
            );
            savingsGoalDao.save(newGoal);
        }

        // 4. Restore Transactions
        for (Transaction t : snapshot.getTransactions()) {
            Category matchCat = categoryDao.findByName(targetUserId, t.getCategoryName(), t.getType())
                    .orElse(null);
            Long catId = matchCat != null ? matchCat.getId() : 1L;

            if (t.getType() == TransactionType.INCOME) {
                Income income = new Income(null, targetUserId, catId, t.getCategoryName(),
                        t.getAmount(), t.getDate(), t.getPaymentMode(), t.getNotes(), null, t.getCreatedAt());
                transactionDao.save(income);
            } else {
                Expense expense = new Expense(null, targetUserId, catId, t.getCategoryName(),
                        t.getAmount(), t.getDate(), t.getPaymentMode(), t.getNotes(), null, t.getCreatedAt());
                transactionDao.save(expense);
            }
        }

        LOGGER.log(Level.INFO, "Backup restored successfully: {0} transactions imported.",
                snapshot.getTransactions().size());
        return snapshot;
    }
}
