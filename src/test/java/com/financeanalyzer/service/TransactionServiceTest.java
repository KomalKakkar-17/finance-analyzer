package com.financeanalyzer.service;

import com.financeanalyzer.dao.jdbc.DatabaseManager;
import com.financeanalyzer.dao.jdbc.JdbcCategoryDao;
import com.financeanalyzer.dao.jdbc.JdbcTransactionDao;
import com.financeanalyzer.dao.jdbc.JdbcUserDao;
import com.financeanalyzer.exception.InvalidTransactionException;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.Expense;
import com.financeanalyzer.model.Income;
import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.User;
import com.financeanalyzer.model.enums.TransactionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceTest {

    private static TransactionService transactionService;
    private static JdbcCategoryDao categoryDao;
    private static JdbcTransactionDao transactionDao;
    private static Long testUserId;

    @BeforeAll
    static void setUpAll() throws Exception {
        DatabaseManager.initializeDatabase();
        categoryDao = new JdbcCategoryDao();
        transactionDao = new JdbcTransactionDao();
        transactionService = new TransactionService(transactionDao, categoryDao);

        JdbcUserDao userDao = new JdbcUserDao();
        User user = userDao.findByUsername("testuser_txn").orElseGet(() -> {
            User u = new User("testuser_txn", "hash123456");
            return userDao.save(u);
        });
        testUserId = user.getId();
    }

    @Test
    void testAddIncomeAndExpensePolymorphism() throws InvalidTransactionException {
        Category salaryCat = categoryDao.findByName(testUserId, "Salary", TransactionType.INCOME).orElseThrow();
        Category foodCat = categoryDao.findByName(testUserId, "Food & Dining", TransactionType.EXPENSE).orElseThrow();

        // 1. Add Income
        Income inc = transactionService.addIncome(testUserId, salaryCat.getId(),
                new BigDecimal("50000.00"), LocalDate.now(), "Bank Transfer", "Monthly Salary");
        assertNotNull(inc.getId());
        assertEquals(new BigDecimal("50000.00"), inc.getAmount());
        assertEquals(new BigDecimal("50000.00"), inc.getSignedAmount()); // Incomes return positive signed amount

        // 2. Add Expense
        Expense exp = transactionService.addExpense(testUserId, foodCat.getId(),
                new BigDecimal("1200.00"), LocalDate.now(), "UPI", "Dinner");
        assertNotNull(exp.getId());
        assertEquals(new BigDecimal("1200.00"), exp.getAmount());
        assertEquals(new BigDecimal("-1200.00"), exp.getSignedAmount()); // Expenses return negative signed amount
    }

    @Test
    void testFutureExpenseDateValidation() {
        Category foodCat = categoryDao.findByName(testUserId, "Food & Dining", TransactionType.EXPENSE).orElseThrow();

        // Attempting to record an expense in the future should throw InvalidTransactionException
        LocalDate futureDate = LocalDate.now().plusDays(2);
        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.addExpense(testUserId, foodCat.getId(),
                    new BigDecimal("500.00"), futureDate, "Cash", "Future Expense");
        });
    }

    @Test
    void testInvalidAmountValidation() {
        Category foodCat = categoryDao.findByName(testUserId, "Food & Dining", TransactionType.EXPENSE).orElseThrow();

        // Zero or negative amounts must fail validation
        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.addExpense(testUserId, foodCat.getId(),
                    BigDecimal.ZERO, LocalDate.now(), "Cash", "Zero amount");
        });

        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.addExpense(testUserId, foodCat.getId(),
                    new BigDecimal("-100.00"), LocalDate.now(), "Cash", "Negative amount");
        });
    }

    @Test
    void testCustomCategoryCreationAndDuplicateRejection() {
        String uniqueCat = "CustomGourmet_" + System.currentTimeMillis();
        Category created = transactionService.addCustomCategory(testUserId, uniqueCat, TransactionType.EXPENSE);
        assertNotNull(created.getId());
        assertEquals(uniqueCat, created.getName());

        // Duplicate case-insensitive creation should be rejected
        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.addCustomCategory(testUserId, uniqueCat.toLowerCase(), TransactionType.EXPENSE);
        });
    }
}
