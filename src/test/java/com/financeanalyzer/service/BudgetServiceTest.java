package com.financeanalyzer.service;

import com.financeanalyzer.dao.jdbc.DatabaseManager;
import com.financeanalyzer.dao.jdbc.JdbcBudgetDao;
import com.financeanalyzer.dao.jdbc.JdbcCategoryDao;
import com.financeanalyzer.dao.jdbc.JdbcTransactionDao;
import com.financeanalyzer.dao.jdbc.JdbcUserDao;
import com.financeanalyzer.exception.BudgetExceededException;
import com.financeanalyzer.model.Budget;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.User;
import com.financeanalyzer.model.enums.AlertLevel;
import com.financeanalyzer.model.enums.TransactionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BudgetServiceTest {

    private static BudgetService budgetService;
    private static TransactionService transactionService;
    private static JdbcCategoryDao categoryDao;
    private static Long testUserId;

    @BeforeAll
    static void setUpAll() {
        DatabaseManager.initializeDatabase();
        categoryDao = new JdbcCategoryDao();
        JdbcTransactionDao transactionDao = new JdbcTransactionDao();
        JdbcBudgetDao budgetDao = new JdbcBudgetDao();
        budgetService = new BudgetService(budgetDao, transactionDao, categoryDao);
        transactionService = new TransactionService(transactionDao, categoryDao);

        JdbcUserDao userDao = new JdbcUserDao();
        User user = userDao.findByUsername("testuser_budget").orElseGet(() -> {
            User u = new User("testuser_budget", "hash123456");
            return userDao.save(u);
        });
        testUserId = user.getId();
    }

    @Test
    void testSetAndRetrieveBudget() {
        Category rentCat = categoryDao.findByName(testUserId, "Rent", TransactionType.EXPENSE).orElseThrow();
        YearMonth testMonth = YearMonth.of(2026, 11);

        Budget budget = budgetService.setBudget(testUserId, rentCat.getId(), testMonth, new BigDecimal("25000.00"));
        assertNotNull(budget.getId());
        assertEquals(new BigDecimal("25000.00"), budget.getLimit());

        Optional<Budget> retrieved = budgetService.getBudgetByCategory(testUserId, rentCat.getId(), testMonth);
        assertTrue(retrieved.isPresent());
        assertEquals(new BigDecimal("25000.00"), retrieved.get().getLimit());
    }

    @Test
    void testAlertThresholdLevels() throws Exception {
        Category foodCat = categoryDao.findByName(testUserId, "Food & Dining", TransactionType.EXPENSE).orElseThrow();
        YearMonth testMonth = YearMonth.of(2026, 12);

        // Set budget limit of ₹10,000
        budgetService.setBudget(testUserId, foodCat.getId(), testMonth, new BigDecimal("10000.00"));

        // 1. Under 80% (e.g. ₹5,000 = 50%) -> AlertLevel.NORMAL
        AlertLevel level1 = budgetService.checkBudgetAlert(testUserId, foodCat.getId(), testMonth, new BigDecimal("5000.00"));
        assertEquals(AlertLevel.NORMAL, level1);

        // 2. Between 80% and 99.9% (e.g. ₹8,500 = 85%) -> AlertLevel.WARNING (Amber)
        AlertLevel level2 = budgetService.checkBudgetAlert(testUserId, foodCat.getId(), testMonth, new BigDecimal("8500.00"));
        assertEquals(AlertLevel.WARNING, level2);

        // 3. At or exceeding 100% (e.g. ₹10,500 = 105%) -> AlertLevel.EXCEEDED (Red)
        AlertLevel level3 = budgetService.checkBudgetAlert(testUserId, foodCat.getId(), testMonth, new BigDecimal("10500.00"));
        assertEquals(AlertLevel.EXCEEDED, level3);
    }
}
