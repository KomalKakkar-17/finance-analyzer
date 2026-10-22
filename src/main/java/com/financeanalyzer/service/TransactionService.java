package com.financeanalyzer.service;

import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.dao.jdbc.DatabaseManager;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.exception.InvalidTransactionException;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.Expense;
import com.financeanalyzer.model.Income;
import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;
import com.financeanalyzer.util.Validators;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service encapsulating business logic for financial transactions and categories.
 * Demonstrates Unit 1 OOP, Unit 2 Collections (ArrayList, LinkedList, HashSet, HashMap),
 * and Unit 4 JDBC transactional commit/rollback.
 */
public class TransactionService {

    private static final Logger LOGGER = AppLogger.getLogger(TransactionService.class);

    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;

    // Recent activity in-memory cache/queue demonstrating Unit 2 LinkedList
    private final LinkedList<String> recentActivityQueue = new LinkedList<>();
    private static final int MAX_ACTIVITY_QUEUE_SIZE = 20;

    public TransactionService(TransactionDao transactionDao, CategoryDao categoryDao) {
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null.");
        this.categoryDao = Objects.requireNonNull(categoryDao, "CategoryDao must not be null.");
    }

    /**
     * Adds an Income transaction.
     */
    public Income addIncome(Long userId, Long categoryId, BigDecimal amount, LocalDate date,
                            String paymentMode, String notes) throws InvalidTransactionException {
        Category cat = categoryDao.findById(categoryId)
                .orElseThrow(() -> new InvalidTransactionException("Category with ID " + categoryId + " does not exist."));

        Validators.validateIncome(amount, date, cat);

        Income income = new Income(null, userId, categoryId, cat.getName(), amount, date,
                paymentMode, notes, null, LocalDateTime.now());

        Transaction saved = transactionDao.save(income);
        recordActivity(String.format("Added Income: +%s (%s)", MoneyUtil.formatCurrency(amount), cat.getName()));
        LOGGER.log(Level.INFO, "User {0} added income of {1} under {2}",
                new Object[]{userId, MoneyUtil.formatCurrency(amount), cat.getName()});

        return (Income) saved;
    }

    /**
     * Adds an Expense transaction.
     * Demonstrates Unit 4 explicit ACID transaction with setAutoCommit(false), commit, and rollback.
     */
    public Expense addExpense(Long userId, Long categoryId, BigDecimal amount, LocalDate date,
                             String paymentMode, String notes) throws InvalidTransactionException {
        Category cat = categoryDao.findById(categoryId)
                .orElseThrow(() -> new InvalidTransactionException("Category with ID " + categoryId + " does not exist."));

        Validators.validateExpense(amount, date, cat);

        // Execute save within an explicit database transaction
        Expense expense = new Expense(null, userId, categoryId, cat.getName(), amount, date,
                paymentMode, notes, null, LocalDateTime.now());

        String sql = """
            INSERT INTO transactions (user_id, category_id, type, amount_paise, txn_date, payment_mode, notes, recurring_rule_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Begin transaction
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setLong(2, categoryId);
                ps.setString(3, TransactionType.EXPENSE.name());
                ps.setLong(4, MoneyUtil.toPaise(amount));
                ps.setString(5, DateUtil.toIsoString(date));
                ps.setString(6, paymentMode);
                ps.setString(7, notes);
                ps.setString(8, expense.getCreatedAt().toString());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        expense.setId(rs.getLong(1));
                    }
                }

                conn.commit(); // Commit transaction
                LOGGER.log(Level.INFO, "Committed expense transaction {0} of {1} for user {2}",
                        new Object[]{expense.getId(), MoneyUtil.formatCurrency(amount), userId});
            } catch (SQLException ex) {
                conn.rollback(); // Rollback transaction on failure
                LOGGER.log(Level.SEVERE, "Rolled back expense transaction due to error: " + ex.getMessage(), ex);
                throw new DataAccessException("Failed to record expense transaction atomically.", ex);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error saving expense.", e);
            throw new DataAccessException("Database error saving expense.", e);
        }

        recordActivity(String.format("Added Expense: -%s (%s)", MoneyUtil.formatCurrency(amount), cat.getName()));
        return expense;
    }

    /**
     * Updates an existing transaction.
     */
    public boolean updateTransaction(Transaction transaction) throws InvalidTransactionException {
        Objects.requireNonNull(transaction, "Transaction cannot be null.");
        Category cat = categoryDao.findById(transaction.getCategoryId())
                .orElseThrow(() -> new InvalidTransactionException("Invalid category for transaction."));

        if (transaction.getType() == TransactionType.EXPENSE) {
            Validators.validateExpense(transaction.getAmount(), transaction.getDate(), cat);
        } else {
            Validators.validateIncome(transaction.getAmount(), transaction.getDate(), cat);
        }

        boolean updated = transactionDao.update(transaction);
        if (updated) {
            recordActivity(String.format("Updated %s: %s (%s)",
                    transaction.getType(), MoneyUtil.formatCurrency(transaction.getAmount()), cat.getName()));
            LOGGER.log(Level.INFO, "Updated transaction ID {0} for user {1}",
                    new Object[]{transaction.getId(), transaction.getUserId()});
        }
        return updated;
    }

    /**
     * Deletes a transaction scoped to a user.
     */
    public boolean deleteTransaction(Long id, Long userId) {
        Optional<Transaction> txn = transactionDao.findById(id);
        boolean deleted = transactionDao.deleteById(id, userId);
        if (deleted && txn.isPresent()) {
            Transaction t = txn.get();
            recordActivity(String.format("Deleted %s: %s", t.getType(), MoneyUtil.formatCurrency(t.getAmount())));
            LOGGER.log(Level.INFO, "Deleted transaction ID {0} for user {1}", new Object[]{id, userId});
        }
        return deleted;
    }

    /**
     * Finds transactions based on user filter criteria.
     */
    public List<Transaction> findFiltered(Long userId, LocalDate fromDate, LocalDate toDate,
                                          Long categoryId, TransactionType type,
                                          BigDecimal minAmount, BigDecimal maxAmount, String keyword) {
        return transactionDao.findFiltered(userId, fromDate, toDate, categoryId, type, minAmount, maxAmount, keyword);
    }

    /**
     * Retrieves all transactions for user.
     */
    public List<Transaction> getAllTransactions(Long userId) {
        return transactionDao.findAll(userId);
    }

    /**
     * Retrieves recent transactions.
     */
    public List<Transaction> getRecentTransactions(Long userId, int limit) {
        return transactionDao.findRecent(userId, limit);
    }

    /**
     * Retrieves categories for dropdowns.
     */
    public List<Category> getCategories(Long userId, TransactionType type) {
        return categoryDao.findCategoriesForUser(userId, type);
    }

    /**
     * Adds a custom category for the user, rejecting duplicates (case-insensitive).
     */
    public Category addCustomCategory(Long userId, String name, TransactionType type) throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Category type must be specified.");
        }

        String trimmed = name.trim();
        Optional<Category> existing = categoryDao.findByName(userId, trimmed, type);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A category named '" + trimmed + "' already exists for " + type + ".");
        }

        Category cat = new Category(null, userId, trimmed, type, false);
        Category saved = categoryDao.save(cat);
        LOGGER.log(Level.INFO, "Created custom category '{0}' ({1}) for user {2}",
                new Object[]{trimmed, type, userId});
        return saved;
    }

    /**
     * Demonstrates Unit 2 Collections (HashSet) for collecting distinct payment modes.
     */
    public Set<String> getUniquePaymentModes(Long userId) {
        List<Transaction> list = transactionDao.findAll(userId);
        Set<String> modes = new HashSet<>();
        for (Transaction t : list) {
            if (t.getPaymentMode() != null && !t.getPaymentMode().isBlank()) {
                modes.add(t.getPaymentMode());
            }
        }
        return modes;
    }

    /**
     * Demonstrates Unit 2 Collections (HashMap) for category totals.
     */
    public Map<Category, BigDecimal> getCategoryTotals(Long userId, YearMonth yearMonth) {
        List<Category> categories = categoryDao.findCategoriesForUser(userId, TransactionType.EXPENSE);
        Map<Category, BigDecimal> map = new HashMap<>();
        for (Category cat : categories) {
            BigDecimal spent = transactionDao.getCategorySpendInMonth(userId, cat.getId(), yearMonth);
            map.put(cat, spent);
        }
        return map;
    }

    /**
     * Appends an event to the recent activity queue (LinkedList).
     */
    public synchronized void recordActivity(String activity) {
        if (activity == null) return;
        recentActivityQueue.addFirst(LocalDateTime.now().format(DateUtil.DISPLAY_DATE_FORMATTER) + " - " + activity);
        while (recentActivityQueue.size() > MAX_ACTIVITY_QUEUE_SIZE) {
            recentActivityQueue.removeLast();
        }
    }

    /**
     * Retrieves recent activity queue as an unmodifiable list.
     */
    public synchronized List<String> getRecentActivityList() {
        return Collections.unmodifiableList(new ArrayList<>(recentActivityQueue));
    }
}
