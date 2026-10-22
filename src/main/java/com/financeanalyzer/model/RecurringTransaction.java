package com.financeanalyzer.model;

import com.financeanalyzer.model.enums.Frequency;
import com.financeanalyzer.model.enums.RuleStatus;
import com.financeanalyzer.model.enums.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing an automated recurring transaction rule.
 */
public class RecurringTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long categoryId;
    private String categoryName; // Denormalized for UI convenience
    private TransactionType type;
    private BigDecimal amount;
    private Frequency frequency;
    private LocalDate startDate;
    private LocalDate nextDueDate;
    private LocalDate endDate;
    private RuleStatus status;

    public RecurringTransaction() {
        this.status = RuleStatus.ACTIVE;
    }

    public RecurringTransaction(Long id, Long userId, Long categoryId, String categoryName,
                                TransactionType type, BigDecimal amount, Frequency frequency,
                                LocalDate startDate, LocalDate nextDueDate, LocalDate endDate,
                                RuleStatus status) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.type = type;
        this.amount = amount;
        this.frequency = frequency;
        this.startDate = startDate;
        this.nextDueDate = nextDueDate;
        this.endDate = endDate;
        this.status = status;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecurringTransaction that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, categoryId);
    }

    @Override
    public String toString() {
        return "RecurringTransaction{" +
                "id=" + id +
                ", type=" + type +
                ", amount=" + amount +
                ", frequency=" + frequency +
                ", nextDueDate=" + nextDueDate +
                ", status=" + status +
                '}';
    }
}
