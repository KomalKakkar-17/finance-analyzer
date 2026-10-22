package com.financeanalyzer.service;

import com.financeanalyzer.model.User;
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

    public HeuristicEngine() {
        this.rules = new ArrayList<>();
        // Register the 5 syllabus heuristic rules
        rules.add(new HighCategorySpendRule());
        rules.add(new HighExpenseToIncomeRule());
        rules.add(new CategorySpikeRule());
        rules.add(new LowSavingsRateRule());
        rules.add(new FrequentMicroTransactionsRule());
    }

    public HeuristicEngine(List<Recommendation> customRules) {
        this.rules = Objects.requireNonNullElseGet(customRules, ArrayList::new);
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
