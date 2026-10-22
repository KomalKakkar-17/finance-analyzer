package com.financeanalyzer.service;

import com.financeanalyzer.model.User;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.rules.*;
import com.financeanalyzer.util.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Analytical rule-based recommendation engine.
 * Evaluates configured heuristic rules and respects the per-user toggle (FR-13).
 */
public class HeuristicEngine {

    private static final Logger LOGGER = AppLogger.getLogger(HeuristicEngine.class);

    private final List<Recommendation> rules;
    private final CategoryService categoryService;

    public HeuristicEngine() {
        this.categoryService = null;
        this.rules = defaultRules();
    }

    public HeuristicEngine(List<Recommendation> customRules) {
        this.categoryService = null;
        this.rules = Objects.requireNonNullElseGet(customRules, ArrayList::new);
    }

    public HeuristicEngine(CategoryService categoryService) {
        this.categoryService = Objects.requireNonNull(categoryService, "CategoryService must not be null.");
        this.rules = defaultRules();
    }

    private static List<Recommendation> defaultRules() {
        return new ArrayList<>(List.of(
                new HighCategorySpendRule(),
                new HighExpenseToIncomeRule(),
                new CategorySpikeRule(),
                new LowSavingsRateRule(),
                new FrequentMicroTransactionsRule()
        ));
    }

    /**
     * Evaluates all heuristic rules against the financial context.
     * Respects user's recommendations preference setting (FR-13).
     *
     * @param user user entity
     * @param ctx financial analysis context
     * @return list of triggered recommendation strings
     */
    public List<String> generateRecommendations(User user, AnalysisContext ctx) {
        if (user == null || !user.isRecommendationsEnabled()) {
            LOGGER.info("Smart recommendations are disabled by user preference.");
            return List.of();
        }

        if (ctx == null) {
            return List.of();
        }

        if (categoryService != null) {
            List<Category> expenseCategories = categoryService.findExpenseCategories(user.getId());
            java.util.Map<Long, String> categoryNamesById = new java.util.HashMap<>();
            for (Category category : expenseCategories) {
                categoryNamesById.put(category.getId(), category.getName());
            }
            ctx = ctx.withFixedCategoryData(
                    categoryService.findFixedCategoryIds(user.getId()),
                    ctx.getCurrentMonthCategoryExpensesById(),
                    ctx.getLastMonthCategoryExpensesById(),
                    categoryNamesById);
        }

        List<String> recommendations = new ArrayList<>();
        for (Recommendation rule : rules) {
            try {
                Optional<String> advice = rule.evaluate(ctx);
                advice.ifPresent(recommendations::add);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error evaluating heuristic rule: " + rule.getRuleName(), ex);
            }
        }

        LOGGER.log(Level.INFO, "Evaluated {0} heuristic rules, generated {1} active recommendations for user {2}",
                new Object[]{rules.size(), recommendations.size(), user.getUsername()});
        return recommendations;
    }

    public List<Recommendation> getRegisteredRules() {
        return List.copyOf(rules);
    }
}
