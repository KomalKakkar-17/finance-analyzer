package com.financeanalyzer.model;

import com.financeanalyzer.model.enums.AlertLevel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object carrying real-time analytical metrics for the in-modal spend preview card.
 * Carries month-to-date category spend, budget limit, projected utilisation, alert level, and goal impacts.
 */
public class AnalyticsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal categorySpent;
    private BigDecimal budgetLimit;
    private BigDecimal remainingBudget;
    private BigDecimal projectedUtilisationPercent;
    private AlertLevel alertLevel;
    private boolean hasBudget;
    private BigDecimal proposedAmount;
    private BigDecimal monthlySavingsBefore;
    private BigDecimal monthlySavingsAfter;
    private List<GoalImpact> goalImpacts;

    public AnalyticsDTO() {
        this.categorySpent = BigDecimal.ZERO;
        this.budgetLimit = BigDecimal.ZERO;
        this.remainingBudget = BigDecimal.ZERO;
        this.projectedUtilisationPercent = BigDecimal.ZERO;
        this.alertLevel = AlertLevel.NORMAL;
        this.hasBudget = false;
        this.proposedAmount = BigDecimal.ZERO;
        this.monthlySavingsBefore = BigDecimal.ZERO;
        this.monthlySavingsAfter = BigDecimal.ZERO;
        this.goalImpacts = new ArrayList<>();
    }

    public AnalyticsDTO(BigDecimal categorySpent, BigDecimal budgetLimit, BigDecimal remainingBudget,
                        BigDecimal projectedUtilisationPercent, AlertLevel alertLevel, boolean hasBudget,
                        BigDecimal proposedAmount, BigDecimal monthlySavingsBefore, BigDecimal monthlySavingsAfter,
                        List<GoalImpact> goalImpacts) {
        this.categorySpent = categorySpent;
        this.budgetLimit = budgetLimit;
        this.remainingBudget = remainingBudget;
        this.projectedUtilisationPercent = projectedUtilisationPercent;
        this.alertLevel = alertLevel;
        this.hasBudget = hasBudget;
        this.proposedAmount = proposedAmount;
        this.monthlySavingsBefore = monthlySavingsBefore;
        this.monthlySavingsAfter = monthlySavingsAfter;
        this.goalImpacts = goalImpacts != null ? goalImpacts : new ArrayList<>();
    }

    public BigDecimal getCategorySpent() {
        return categorySpent;
    }

    public void setCategorySpent(BigDecimal categorySpent) {
        this.categorySpent = categorySpent;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public BigDecimal getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(BigDecimal remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public BigDecimal getProjectedUtilisationPercent() {
        return projectedUtilisationPercent;
    }

    public void setProjectedUtilisationPercent(BigDecimal projectedUtilisationPercent) {
        this.projectedUtilisationPercent = projectedUtilisationPercent;
    }

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }

    public boolean isHasBudget() {
        return hasBudget;
    }

    public void setHasBudget(boolean hasBudget) {
        this.hasBudget = hasBudget;
    }

    public BigDecimal getProposedAmount() {
        return proposedAmount;
    }

    public void setProposedAmount(BigDecimal proposedAmount) {
        this.proposedAmount = proposedAmount;
    }

    public BigDecimal getMonthlySavingsBefore() {
        return monthlySavingsBefore;
    }

    public void setMonthlySavingsBefore(BigDecimal monthlySavingsBefore) {
        this.monthlySavingsBefore = monthlySavingsBefore;
    }

    public BigDecimal getMonthlySavingsAfter() {
        return monthlySavingsAfter;
    }

    public void setMonthlySavingsAfter(BigDecimal monthlySavingsAfter) {
        this.monthlySavingsAfter = monthlySavingsAfter;
    }

    public List<GoalImpact> getGoalImpacts() {
        return Collections.unmodifiableList(goalImpacts);
    }

    public void setGoalImpacts(List<GoalImpact> goalImpacts) {
        this.goalImpacts = goalImpacts != null ? new ArrayList<>(goalImpacts) : new ArrayList<>();
    }

    /**
     * Details regarding how the proposed expense affects a specific active savings goal.
     */
    public static class GoalImpact implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String goalTitle;
        private final BigDecimal targetAmount;
        private final BigDecimal currentSaved;
        private final int allocationRatio;
        private final BigDecimal monthlyContributionReduction; // Amount reduced from goal's share of surplus
        private final String message;

        public GoalImpact(String goalTitle, BigDecimal targetAmount, BigDecimal currentSaved,
                          int allocationRatio, BigDecimal monthlyContributionReduction, String message) {
            this.goalTitle = goalTitle;
            this.targetAmount = targetAmount;
            this.currentSaved = currentSaved;
            this.allocationRatio = allocationRatio;
            this.monthlyContributionReduction = monthlyContributionReduction;
            this.message = message;
        }

        public String getGoalTitle() {
            return goalTitle;
        }

        public BigDecimal getTargetAmount() {
            return targetAmount;
        }

        public BigDecimal getCurrentSaved() {
            return currentSaved;
        }

        public int getAllocationRatio() {
            return allocationRatio;
        }

        public BigDecimal getMonthlyContributionReduction() {
            return monthlyContributionReduction;
        }

        public String getMessage() {
            return message;
        }
    }
}
