package com.financeanalyzer.service;

import com.financeanalyzer.model.AnalyticsDTO;
import com.financeanalyzer.model.Category;

import java.math.BigDecimal;

/**
 * Service calculating real-time in-modal analytical projections for live spend previews.
 */
public interface InModalAnalyticsService {

    /**
     * Calculates projected month-to-date category spend, budget utilization percentage,
     * alert level, and goal impact.
     *
     * @param userId user ID
     * @param category category being charged
     * @param proposedAmount amount entered into modal
     * @param isExpense whether the transaction is an expense
     * @return populated AnalyticsDTO
     */
    AnalyticsDTO calculatePreview(Long userId, Category category, BigDecimal proposedAmount, boolean isExpense);
}
