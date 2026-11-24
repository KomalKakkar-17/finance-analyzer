package com.financeanalyzer.service;

import com.financeanalyzer.dao.BudgetDao;
import com.financeanalyzer.dao.SavingsGoalDao;
import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.model.AnalyticsDTO;
import com.financeanalyzer.model.Budget;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.SavingsGoal;
import com.financeanalyzer.model.enums.AlertLevel;
import com.financeanalyzer.model.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demonstrates two test-design techniques on one function,
 * {@link DefaultInModalAnalyticsService#calculatePreview}:
 *
 * <ul>
 *   <li><b>White-box</b>: basis-path testing from the control flow graph.
 *       V(G) = 11, so eleven independent paths (WB-01 to WB-11).</li>
 *   <li><b>Black-box</b>: equivalence partitioning (EP-xx) and boundary value analysis (BV-xx)
 *       using only the inputs and the documented outputs.</li>
 * </ul>
 *
 * The three DAOs are replaced by in-memory stubs (dynamic proxies), so no database is needed
 * and every case is deterministic. The list of DAO calls made is also asserted, which proves
 * which branch of the code was executed.
 */
class InModalAnalyticsServiceTest {

    // ------------------------------------------------------------------ fixture

    /** Data the stub DAOs return, plus a log of the DAO methods that were called. */
    private static final class Fixture {
        BigDecimal spent = money("7000.00");
        Optional<Budget> budget = Optional.of(new Budget(1L, 10L, YearMonth.now(), money("10000.00")));
        BigDecimal income = money("50000.00");
        BigDecimal expense = money("7000.00");
        List<SavingsGoal> goals = new ArrayList<>(List.of(goal("Laptop Fund", 60)));
        final List<String> calls = new ArrayList<>();

        Fixture spent(String v) { spent = money(v); return this; }
        Fixture limit(String v) { budget = Optional.of(new Budget(1L, 10L, YearMonth.now(), money(v))); return this; }
        Fixture noBudget() { budget = Optional.empty(); return this; }
        Fixture goals(SavingsGoal... g) { goals = new ArrayList<>(List.of(g)); return this; }

        DefaultInModalAnalyticsService service() {
            TransactionDao t = stub(TransactionDao.class, calls, name -> switch (name) {
                case "getCategorySpendInMonth" -> spent;
                case "getTotalIncomeInMonth" -> income;
                case "getTotalExpenseInMonth" -> expense;
                default -> throw new UnsupportedOperationException(name);
            });
            BudgetDao b = stub(BudgetDao.class, calls, name -> {
                if (name.equals("findByCategoryAndMonth")) return budget;
                throw new UnsupportedOperationException(name);
            });
            SavingsGoalDao s = stub(SavingsGoalDao.class, calls, name -> {
                if (name.equals("findActiveGoals")) return goals;
                throw new UnsupportedOperationException(name);
            });
            return new DefaultInModalAnalyticsService(b, t, s);
        }
    }

    private interface Answer { Object get(String methodName); }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, List<String> calls, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            calls.add(method.getName());
            return answer.get(method.getName());
        });
    }

    private static BigDecimal money(String v) { return new BigDecimal(v); }

    private static SavingsGoal goal(String title, int ratio) {
        return new SavingsGoal(null, 1L, title, money("60000.00"), money("0.00"),
                LocalDate.now().plusMonths(6), ratio, true);
    }

    private static final Category FOOD = new Category(10L, 1L, "Food & Dining", TransactionType.EXPENSE, true);
    private static final Category NO_ID = new Category("Unsaved", TransactionType.EXPENSE);

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private static List<String> callsOf(String... names) { return List.of(names); }

    private static final String SPEND = "getCategorySpendInMonth";
    private static final String BUDGET = "findByCategoryAndMonth";
    private static final String INCOME = "getTotalIncomeInMonth";
    private static final String EXPENSE = "getTotalExpenseInMonth";
    private static final String GOALS = "findActiveGoals";

    // =================================================================== WHITE-BOX
    // Baseline data: spent 7,000; budget limit 10,000; income 50,000; expenses 7,000;
    // one active goal "Laptop Fund" at 60%.  Proposed expense 2,000 in Food & Dining.

    @Test
    @DisplayName("WB-01 baseline path: expense with budget and one goal")
    void wb01_baseline() {
        Fixture f = new Fixture();
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("2000.00"), true);

        assertMoney("7000.00", r.getCategorySpent());
        assertMoney("10000.00", r.getBudgetLimit());
        assertMoney("3000.00", r.getRemainingBudget());
        assertMoney("90.0", r.getProjectedUtilisationPercent());
        assertEquals(AlertLevel.WARNING, r.getAlertLevel());
        assertTrue(r.isHasBudget());
        assertMoney("2000.00", r.getProposedAmount());
        assertMoney("43000.00", r.getMonthlySavingsBefore());
        assertMoney("41000.00", r.getMonthlySavingsAfter());
        assertEquals(1, r.getGoalImpacts().size());
        assertMoney("1200.00", r.getGoalImpacts().get(0).getMonthlyContributionReduction());
        assertEquals(callsOf(SPEND, BUDGET, INCOME, EXPENSE, GOALS), f.calls);
    }

    @Test
    @DisplayName("WB-02 decision 1 false: userId is null, empty result and no DAO call")
    void wb02_userIdNull() {
        Fixture f = new Fixture();
        AnalyticsDTO r = f.service().calculatePreview(null, FOOD, money("2000.00"), true);

        assertMoney("0", r.getCategorySpent());
        assertFalse(r.isHasBudget());
        assertEquals(AlertLevel.NORMAL, r.getAlertLevel());
        assertTrue(r.getGoalImpacts().isEmpty());
        assertTrue(f.calls.isEmpty(), "no DAO should be touched");
    }

    @Test
    @DisplayName("WB-03 decision 2: negative amount is treated as zero; goals skipped")
    void wb03_amountInvalid() {
        Fixture f = new Fixture();
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("-50.00"), true);

        assertMoney("0", r.getProposedAmount());
        assertMoney("70.0", r.getProjectedUtilisationPercent());
        assertEquals(AlertLevel.NORMAL, r.getAlertLevel());
        assertMoney("43000.00", r.getMonthlySavingsAfter());
        assertTrue(r.getGoalImpacts().isEmpty());
        assertEquals(callsOf(SPEND, BUDGET, INCOME, EXPENSE), f.calls);
    }

    @Test
    @DisplayName("WB-04 decision 3: category is null, budget lookup skipped")
    void wb04_categoryNull() {
        Fixture f = new Fixture();
        AnalyticsDTO r = f.service().calculatePreview(1L, null, money("2000.00"), true);

        assertFalse(r.isHasBudget());
        assertMoney("0", r.getCategorySpent());
        assertMoney("0", r.getProjectedUtilisationPercent());
        assertEquals(AlertLevel.NORMAL, r.getAlertLevel());
        assertEquals(1, r.getGoalImpacts().size());
        assertEquals(callsOf(INCOME, EXPENSE, GOALS), f.calls);
    }

    @Test
    @DisplayName("WB-05 decision 4: category has no budget this month")
    void wb05_noBudget() {
        Fixture f = new Fixture().noBudget();
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("2000.00"), true);

        assertFalse(r.isHasBudget());
        assertMoney("7000.00", r.getCategorySpent());
        assertMoney("0", r.getBudgetLimit());
        assertMoney("0", r.getProjectedUtilisationPercent());
        assertEquals(AlertLevel.NORMAL, r.getAlertLevel());
        assertEquals(1, r.getGoalImpacts().size());
    }

    @Test
    @DisplayName("WB-06 decision 5: spending already above the limit, remaining clamped to zero")
    void wb06_overspent() {
        Fixture f = new Fixture().spent("12000.00");
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("500.00"), true);

        assertMoney("0", r.getRemainingBudget());
        assertMoney("125.0", r.getProjectedUtilisationPercent());
        assertEquals(AlertLevel.EXCEEDED, r.getAlertLevel());
    }

    @Test
    @DisplayName("WB-07 decision 6: budget limit is zero, utilisation skipped (no division by zero)")
    void wb07_limitZero() {
        Fixture f = new Fixture().spent("0.00").limit("0.00");
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("2000.00"), true);

        assertTrue(r.isHasBudget());
        assertMoney("0", r.getRemainingBudget());
        assertMoney("0", r.getProjectedUtilisationPercent());
        assertEquals(AlertLevel.NORMAL, r.getAlertLevel());
    }

    @Test
    @DisplayName("WB-08 decisions 6, 7, 8: income entry, no utilisation, savings increase, no goals")
    void wb08_income() {
        Fixture f = new Fixture();
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("500.00"), false);

        assertMoney("0", r.getProjectedUtilisationPercent());
        assertMoney("43500.00", r.getMonthlySavingsAfter());
        assertTrue(r.getGoalImpacts().isEmpty());
        assertEquals(callsOf(SPEND, BUDGET, INCOME, EXPENSE), f.calls);
    }

    @Test
    @DisplayName("WB-09 decision 9: loop runs zero times, no active goals")
    void wb09_noGoals() {
        Fixture f = new Fixture().goals();
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("2000.00"), true);

        assertTrue(r.getGoalImpacts().isEmpty());
        assertMoney("90.0", r.getProjectedUtilisationPercent());
        assertEquals(callsOf(SPEND, BUDGET, INCOME, EXPENSE, GOALS), f.calls);
    }

    @Test
    @DisplayName("WB-10 decision 10: goal with 0% ratio is skipped")
    void wb10_ratioZero() {
        Fixture f = new Fixture().goals(goal("Parked goal", 0));
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("2000.00"), true);

        assertTrue(r.getGoalImpacts().isEmpty());
    }

    @Test
    @DisplayName("WB-11 decisions 2 and 8 together: income with no amount typed yet")
    void wb11_incomeWithoutAmount() {
        Fixture f = new Fixture();
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, null, false);

        assertMoney("0", r.getProposedAmount());
        assertMoney("43000.00", r.getMonthlySavingsAfter());
        assertTrue(r.getGoalImpacts().isEmpty());
    }

    // =================================================================== BLACK-BOX
    // Equivalence partitioning: one representative from each class of each input.

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "EP-01 amount valid (>0)          | 2000.00 | 2000.00 | 90.0  | WARNING",
            "EP-02 amount zero                | 0.00    | 0.00    | 70.0  | NORMAL",
            "EP-03 amount negative            | -5.00   | 0.00    | 70.0  | NORMAL",
            "EP-04 amount null (not typed)    | NULL    | 0.00    | 70.0  | NORMAL",
            "EP-05 utilisation class below 80 | 500.00  | 500.00  | 75.0  | NORMAL",
            "EP-06 utilisation class 80..<100 | 2000.00 | 2000.00 | 90.0  | WARNING",
            "EP-07 utilisation class >= 100   | 4000.00 | 4000.00 | 110.0 | EXCEEDED"
    })
    void ep_amountAndUtilisationClasses(String id, String input, String expectedAmount,
                                        String expectedUtil, AlertLevel expectedLevel) {
        BigDecimal amount = input.trim().equals("NULL") ? null : money(input.trim());
        AnalyticsDTO r = new Fixture().service().calculatePreview(1L, FOOD, amount, true);

        assertMoney(expectedAmount.trim(), r.getProposedAmount());
        assertMoney(expectedUtil.trim(), r.getProjectedUtilisationPercent());
        assertEquals(expectedLevel, r.getAlertLevel());
    }

    @Test
    @DisplayName("EP-08 category class: object without an identifier behaves like no category")
    void ep_categoryWithoutId() {
        Fixture f = new Fixture();
        AnalyticsDTO r = f.service().calculatePreview(1L, NO_ID, money("2000.00"), true);

        assertFalse(r.isHasBudget());
        assertEquals(callsOf(INCOME, EXPENSE, GOALS), f.calls);
    }

    @Test
    @DisplayName("EP-09 Boolean class: isExpense=false never produces an alert or goal impact")
    void ep_isExpenseFalse() {
        AnalyticsDTO r = new Fixture().service().calculatePreview(1L, FOOD, money("9999.00"), false);

        assertEquals(AlertLevel.NORMAL, r.getAlertLevel());
        assertTrue(r.getGoalImpacts().isEmpty());
    }

    // Boundary value analysis on the proposed amount.
    // Budget limit 1,000, nothing spent yet, so utilisation = amount / 1,000 x 100,
    // divided to four decimals and rounded half-up to one decimal before the 80/100 test.

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "BV-01 just below zero    | -0.01   | 0.00    | 0.0   | NORMAL",
            "BV-02 zero               | 0.00    | 0.00    | 0.0   | NORMAL",
            "BV-03 smallest amount    | 0.01    | 0.01    | 0.0   | NORMAL",
            "BV-04 just below 80%     | 799.44  | 799.44  | 79.9  | NORMAL",
            "BV-05 rounds up to 80%   | 799.45  | 799.45  | 80.0  | WARNING",
            "BV-06 exactly 80%        | 800.00  | 800.00  | 80.0  | WARNING",
            "BV-07 just below 100%    | 999.44  | 999.44  | 99.9  | WARNING",
            "BV-08 rounds up to 100%  | 999.45  | 999.45  | 100.0 | EXCEEDED",
            "BV-09 exactly 100%       | 1000.00 | 1000.00 | 100.0 | EXCEEDED",
            "BV-10 just above 100%    | 1000.01 | 1000.01 | 100.0 | EXCEEDED",
            "BV-11 well above 100%    | 1100.00 | 1100.00 | 110.0 | EXCEEDED"
    })
    void bv_amountAgainstAlertThresholds(String id, String input, String expectedAmount,
                                         String expectedUtil, AlertLevel expectedLevel) {
        Fixture f = new Fixture().spent("0.00").limit("1000.00");
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money(input.trim()), true);

        assertMoney(expectedAmount.trim(), r.getProposedAmount());
        assertMoney(expectedUtil.trim(), r.getProjectedUtilisationPercent());
        assertEquals(expectedLevel, r.getAlertLevel());
    }

    // Boundary value analysis on remaining budget (limit 10,000).

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "BV-12 spent 0.01 below limit | 9999.99  | 0.01",
            "BV-13 spent equals limit     | 10000.00 | 0.00",
            "BV-14 spent 0.01 above limit | 10000.01 | 0.00"
    })
    void bv_remainingBudget(String id, String spent, String expectedRemaining) {
        Fixture f = new Fixture().spent(spent.trim());
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("1.00"), true);

        assertMoney(expectedRemaining.trim(), r.getRemainingBudget());
    }

    // Boundary value analysis on a goal's allocation ratio (amount 1,000).

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "BV-15 ratio 0 (skipped)    | 0   | NONE",
            "BV-16 ratio 1              | 1   | 10.00",
            "BV-17 ratio 99             | 99  | 990.00",
            "BV-18 ratio 100            | 100 | 1000.00"
    })
    void bv_goalRatio(String id, int ratio, String expectedReduction) {
        Fixture f = new Fixture().goals(goal("Boundary goal", ratio));
        AnalyticsDTO r = f.service().calculatePreview(1L, FOOD, money("1000.00"), true);

        if (expectedReduction.trim().equals("NONE")) {
            assertTrue(r.getGoalImpacts().isEmpty());
        } else {
            assertEquals(1, r.getGoalImpacts().size());
            assertMoney(expectedReduction.trim(), r.getGoalImpacts().get(0).getMonthlyContributionReduction());
        }
    }
}
