package com.financeanalyzer.model;

import com.financeanalyzer.model.enums.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class representing a financial transaction.
 * Demonstrates Unit 1 Object-Oriented Programming (encapsulation, inheritance, polymorphism).
 */
public abstract class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Long id;
    protected Long userId;
    protected Long categoryId;
    protected String categoryName; // Transient / denormalized for UI convenience
    protected BigDecimal amount;
    protected LocalDate date;
    protected String paymentMode;
    protected String notes;
    protected Long recurringRuleId;
    protected LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    public Transaction() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Full parameterized constructor.
     */
    public Transaction(Long id, Long userId, Long categoryId, String categoryName,
                       BigDecimal amount, LocalDate date, String paymentMode,
                       String notes, Long recurringRuleId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.date = date;
        this.paymentMode = paymentMode;
        this.notes = notes;
        this.recurringRuleId = recurringRuleId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Returns the polymorphic transaction type (INCOME or EXPENSE).
     *
     * @return TransactionType
     */
    public abstract TransactionType getType();

    /**
     * Polymorphic method returning the signed financial impact of the transaction.
     * Incomes return a positive value; Expenses return a negative value.
     * Demonstrates dynamic method dispatch and polymorphism.
     *
     * @return signed BigDecimal amount
     */
    public abstract BigDecimal getSignedAmount();

    // Getters and Setters

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getRecurringRuleId() {
        return recurringRuleId;
    }

    public void setRecurringRuleId(Long recurringRuleId) {
        this.recurringRuleId = recurringRuleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(categoryId, that.categoryId) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, categoryId, amount, date);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", type=" + getType() +
                ", amount=" + amount +
                ", date=" + date +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}
