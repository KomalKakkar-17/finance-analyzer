package com.financeanalyzer.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Entity representing a monthly category budget limit.
 */
public class Budget implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long categoryId;
    private String categoryName; // Denormalized for display convenience
    private YearMonth yearMonth;
    private BigDecimal limit;

    public Budget() {
    }

    public Budget(Long id, Long userId, Long categoryId, String categoryName, YearMonth yearMonth, BigDecimal limit) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.yearMonth = yearMonth;
        this.limit = limit;
    }

    public Budget(Long userId, Long categoryId, YearMonth yearMonth, BigDecimal limit) {
        this(null, userId, categoryId, null, yearMonth, limit);
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

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Budget budget)) return false;
        return Objects.equals(id, budget.id) &&
                Objects.equals(userId, budget.userId) &&
                Objects.equals(categoryId, budget.categoryId) &&
                Objects.equals(yearMonth, budget.yearMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, categoryId, yearMonth);
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", categoryName='" + categoryName + '\'' +
                ", yearMonth=" + yearMonth +
                ", limit=" + limit +
                '}';
    }
}
