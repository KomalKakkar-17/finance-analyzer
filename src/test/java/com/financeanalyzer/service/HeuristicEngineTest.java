package com.financeanalyzer.service;

import com.financeanalyzer.model.Expense;
import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.User;
import com.financeanalyzer.rules.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class HeuristicEngineTest {

    private HeuristicEngine heuristicEngine;
    private User testUser;

    @BeforeEach
    void setUp() {
        heuristicEngine = new HeuristicEngine();
        testUser = new User(100L, "test_eval", "hash", true, LocalDateTime.now());
    }

    @Test
    void testRule1HighCategorySpend() {
        // Total expense ₹10,000. 'Rent' is ₹5,000 (50% > 40%)
        Map<String, BigDecimal> currentCats = new HashMap<>();
        currentCats.put("Rent", new BigDecimal("5000.00"));
        currentCats.put("Food", new BigDecimal("3000.00"));
        currentCats.put("Utilities", new BigDecimal("2000.00"));

        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), new BigDecimal("20000.00"), new BigDecimal("10000.00"),
                currentCats, Collections.emptyMap(), Collections.emptyList()
        );

        Optional<String> result = new HighCategorySpendRule().evaluate(ctx);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("High Concentration"));
        assertTrue(result.get().contains("Rent"));
    }

    @Test
    void highCategorySpendSkipsFixedCategoryAndUsesFlexibleDenominator() {
        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), BigDecimal.ZERO, new BigDecimal("1000"),
                Map.of("Rent", new BigDecimal("500"), "Food", new BigDecimal("300"),
                        "Transport", new BigDecimal("200")),
                Collections.emptyMap(), Collections.emptyList(),
                Map.of(1L, new BigDecimal("500"), 2L, new BigDecimal("300"),
                        3L, new BigDecimal("200")),
                Collections.emptyMap()
        ).withFixedCategoryData(Set.of(1L),
                Map.of(1L, new BigDecimal("500"), 2L, new BigDecimal("300"),
                        3L, new BigDecimal("200")),
                Collections.emptyMap(), Map.of(1L, "Rent", 2L, "Food", 3L, "Transport"));

        Optional<String> result = new HighCategorySpendRule().evaluate(ctx);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("'Food'"));
        assertTrue(result.get().contains("60.0%"));
    }

    @Test
    void highCategorySpendDoesNotRecommendWhenAllSpendingIsFixed() {
        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), BigDecimal.ZERO, new BigDecimal("1000"),
                Map.of("Rent", new BigDecimal("1000")), Collections.emptyMap(), Collections.emptyList(),
                Map.of(1L, new BigDecimal("1000")), Collections.emptyMap()
        ).withFixedCategoryData(Set.of(1L), Map.of(1L, new BigDecimal("1000")),
                Collections.emptyMap(), Map.of(1L, "Rent"));

        assertTrue(new HighCategorySpendRule().evaluate(ctx).isEmpty());
    }

    @Test
    void highCategorySpendWithNoFixedCategoriesBehavesAsBefore() {
        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), BigDecimal.ZERO, new BigDecimal("1000"),
                Map.of("Food", new BigDecimal("500"), "Transport", new BigDecimal("500")),
                Collections.emptyMap(), Collections.emptyList()
        );

        Optional<String> result = new HighCategorySpendRule().evaluate(ctx);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("'Food'"));
        assertTrue(result.get().contains("50.0%"));
    }

    @Test
    void testRule2HighExpenseToIncome() {
        // Income ₹10,000. Expense ₹9,500 (95% > 90%)
        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), new BigDecimal("10000.00"), new BigDecimal("9500.00"),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList()
        );

        Optional<String> result = new HighExpenseToIncomeRule().evaluate(ctx);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("Elevated Burn Rate"));
    }

    @Test
    void testRule3CategorySpike() {
        // Last month Food: ₹1,000. This month Food: ₹1,400 (40% increase >= 25%)
        Map<String, BigDecimal> currentCats = Map.of("Food & Dining", new BigDecimal("1400.00"));
        Map<String, BigDecimal> lastCats = Map.of("Food & Dining", new BigDecimal("1000.00"));

        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), new BigDecimal("10000.00"), new BigDecimal("3000.00"),
                currentCats, lastCats, Collections.emptyList()
        );

        Optional<String> result = new CategorySpikeRule().evaluate(ctx);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("Spending Spike"));
    }

    @Test
    void categorySpikeSkipsFixedCategory() {
        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), BigDecimal.ZERO, new BigDecimal("1400"),
                Map.of("Rent", new BigDecimal("1400")),
                Map.of("Rent", new BigDecimal("1000")), Collections.emptyList(),
                Map.of(1L, new BigDecimal("1400")), Map.of(1L, new BigDecimal("1000"))
        ).withFixedCategoryData(Set.of(1L),
                Map.of(1L, new BigDecimal("1400")), Map.of(1L, new BigDecimal("1000")),
                Map.of(1L, "Rent"));

        assertTrue(new CategorySpikeRule().evaluate(ctx).isEmpty());
    }

    @Test
    void categorySpikeWithoutFixedCategoriesBehavesAsBefore() {
        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), BigDecimal.ZERO, new BigDecimal("1400"),
                Map.of("Food", new BigDecimal("1400")),
                Map.of("Food", new BigDecimal("1000")), Collections.emptyList()
        );

        assertTrue(new CategorySpikeRule().evaluate(ctx).orElseThrow().contains("Food"));
    }

    @Test
    void testRule4LowSavingsRate() {
        // Income ₹10,000, Expense ₹9,600 -> Net Savings ₹400 (4% < 10%)
        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), new BigDecimal("10000.00"), new BigDecimal("9600.00"),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList()
        );

        Optional<String> result = new LowSavingsRateRule().evaluate(ctx);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("Low Savings Rate"));
    }

    @Test
    void testRule5FrequentMicroTransactions() {
        // 5 expenses under ₹200 within past 7 days
        List<Transaction> txns = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            txns.add(new Expense(
                    (long) i, 100L, 1L, "Food", new BigDecimal("150.00"),
                    today.minusDays(i), "UPI", "Coffee", null, LocalDateTime.now()
            ));
        }

        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), new BigDecimal("10000.00"), new BigDecimal("2000.00"),
                Collections.emptyMap(), Collections.emptyMap(), txns
        );

        Optional<String> result = new FrequentMicroTransactionsRule().evaluate(ctx);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("Frequent Micro-Spends"));
    }

    @Test
    void testRecommendationsMutedWhenDisabled() {
        testUser.setRecommendationsEnabled(false);

        Map<String, BigDecimal> currentCats = Map.of("Rent", new BigDecimal("6000.00"));
        AnalysisContext ctx = new AnalysisContext(
                100L, YearMonth.now(), new BigDecimal("10000.00"), new BigDecimal("9500.00"),
                currentCats, Collections.emptyMap(), Collections.emptyList()
        );

        List<String> list = heuristicEngine.generateRecommendations(testUser, ctx);
        assertTrue(list.isEmpty(), "Recommendations must be suppressed when user disabled them.");
    }
}
