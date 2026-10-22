package com.financeanalyzer.service;

import com.financeanalyzer.dao.jdbc.DatabaseManager;
import com.financeanalyzer.dao.jdbc.JdbcSavingsGoalDao;
import com.financeanalyzer.dao.jdbc.JdbcUserDao;
import com.financeanalyzer.exception.AllocationRatioException;
import com.financeanalyzer.model.SavingsGoal;
import com.financeanalyzer.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RatioAllocationServiceTest {

    private static GoalService goalService;
    private static RatioAllocationService ratioAllocationService;
    private static JdbcSavingsGoalDao goalDao;
    private static Long testUserId;

    @BeforeAll
    static void setUpAll() {
        DatabaseManager.initializeDatabase();
        goalDao = new JdbcSavingsGoalDao();
        goalService = new GoalService(goalDao);
        ratioAllocationService = new RatioAllocationService(goalDao);

        JdbcUserDao userDao = new JdbcUserDao();
        User user = userDao.findByUsername("testuser_ratio").orElseGet(() -> {
            User u = new User("testuser_ratio", "hash123456");
            return userDao.save(u);
        });
        testUserId = user.getId();
    }

    @Test
    void testRatioCapValidation() throws AllocationRatioException {
        // Clear any existing test goals for user
        List<SavingsGoal> existing = goalDao.findAll(testUserId);
        for (SavingsGoal g : existing) {
            goalDao.deleteById(g.getId(), testUserId);
        }

        // Goal 1: 60% ratio
        SavingsGoal g1 = goalService.createGoal(testUserId, "Vacation Fund",
                new BigDecimal("50000.00"), LocalDate.now().plusMonths(6), 60);
        assertNotNull(g1.getId());

        // Goal 2: 30% ratio (total 90% <= 100%)
        SavingsGoal g2 = goalService.createGoal(testUserId, "Tech Gadget",
                new BigDecimal("20000.00"), LocalDate.now().plusMonths(4), 30);
        assertNotNull(g2.getId());

        // Goal 3: Attempting 20% (60 + 30 + 20 = 110% > 100%) must throw AllocationRatioException
        assertThrows(AllocationRatioException.class, () -> {
            goalService.createGoal(testUserId, "Excess Goal",
                    new BigDecimal("10000.00"), LocalDate.now().plusMonths(3), 20);
        });
    }

    @Test
    void testAtomicDepositAllocation() {
        // Clear test goals
        List<SavingsGoal> existing = goalDao.findAll(testUserId);
        for (SavingsGoal g : existing) {
            goalDao.deleteById(g.getId(), testUserId);
        }

        // Goal A: 50% ratio
        SavingsGoal ga = goalDao.save(new SavingsGoal(testUserId, "Goal A",
                new BigDecimal("100000.00"), LocalDate.now().plusMonths(12), 50));

        // Goal B: 30% ratio
        SavingsGoal gb = goalDao.save(new SavingsGoal(testUserId, "Goal B",
                new BigDecimal("50000.00"), LocalDate.now().plusMonths(10), 30));

        // Total active ratio = 80%. Unallocated = 20%.
        // Deposit ₹10,000
        BigDecimal deposit = new BigDecimal("10000.00");
        RatioAllocationService.AllocationResult result = ratioAllocationService.allocateDeposit(testUserId, deposit);

        assertEquals(new BigDecimal("10000.00"), result.getTotalDeposit());
        assertEquals(new BigDecimal("8000.00"), result.getTotalAllocated()); // 50% + 30% = ₹8,000
        assertEquals(new BigDecimal("2000.00"), result.getUnallocatedRemainder()); // 20% = ₹2,000

        // Verify updated balances in database
        SavingsGoal updatedA = goalDao.findById(ga.getId()).orElseThrow();
        assertEquals(new BigDecimal("5000.00"), updatedA.getSavedAmount());

        SavingsGoal updatedB = goalDao.findById(gb.getId()).orElseThrow();
        assertEquals(new BigDecimal("3000.00"), updatedB.getSavedAmount());
    }
}
