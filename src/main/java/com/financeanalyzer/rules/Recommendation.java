package com.financeanalyzer.rules;

import java.util.Optional;

/**
 * Functional interface representing a financial recommendation heuristic rule.
 * Visibly demonstrates Unit 1 interfaces and Unit 2 Optional return types.
 */
public interface Recommendation {

    /**
     * Evaluates the financial context and returns an actionable recommendation message if triggered.
     *
     * @param ctx analysis context
     * @return Optional containing recommendation text if rule triggered, or empty
     */
    Optional<String> evaluate(AnalysisContext ctx);

    /**
     * Returns the human-readable identifier of this rule.
     */
    String getRuleName();
}
