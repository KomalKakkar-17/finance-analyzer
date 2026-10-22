package com.financeanalyzer.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing a savings goal with target amount, current savings, deadline, and allocation ratio.
 */
public class SavingsGoal implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String title;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private LocalDate deadline;
    private int allocationRatio; // 0 to 100
    private boolean active;

    public SavingsGoal() {
        this.savedAmount = BigDecimal.ZERO;
        this.active = true;
    }

    public SavingsGoal(Long id, Long userId, String title, BigDecimal targetAmount,
                       BigDecimal savedAmount, LocalDate deadline, int allocationRatio, boolean active) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount != null ? savedAmount : BigDecimal.ZERO;
        this.deadline = deadline;
        this.allocationRatio = allocationRatio;
        this.active = active;
    }

    public SavingsGoal(Long userId, String title, BigDecimal targetAmount, LocalDate deadline, int allocationRatio) {
        this(null, userId, title, targetAmount, BigDecimal.ZERO, deadline, allocationRatio, true);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getSavedAmount() {
        return savedAmount;
    }

    public void setSavedAmount(BigDecimal savedAmount) {
        this.savedAmount = savedAmount != null ? savedAmount : BigDecimal.ZERO;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public int getAllocationRatio() {
        return allocationRatio;
    }

    public void setAllocationRatio(int allocationRatio) {
        this.allocationRatio = allocationRatio;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Calculates the remaining amount needed to reach the target.
     *
     * @return remaining BigDecimal (not less than zero)
     */
    public BigDecimal getRemainingAmount() {
        if (targetAmount == null) return BigDecimal.ZERO;
        BigDecimal diff = targetAmount.subtract(savedAmount != null ? savedAmount : BigDecimal.ZERO);
        return diff.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : diff;
    }

    /**
     * Calculates the completion percentage (0.0% to 100.0%).
     *
     * @return progress as a percentage double (0.0 to 100.0)
     */
    public double getProgressPercentage() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        if (savedAmount == null || savedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        BigDecimal pct = savedAmount.divide(targetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return Math.min(100.0, pct.doubleValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavingsGoal that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, title);
    }

    @Override
    public String toString() {
        return "SavingsGoal{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", targetAmount=" + targetAmount +
                ", savedAmount=" + savedAmount +
                ", ratio=" + allocationRatio + "%" +
                ", active=" + active +
                '}';
    }
}
