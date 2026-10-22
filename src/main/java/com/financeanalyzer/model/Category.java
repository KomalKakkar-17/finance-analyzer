package com.financeanalyzer.model;

import com.financeanalyzer.model.enums.TransactionType;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entity representing an income or expense category.
 * Supports both global system default categories (userId == null) and custom user categories.
 */
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId; // null for system defaults
    private String name;
    private TransactionType type;
    private boolean system;

    public Category() {
    }

    public Category(Long id, Long userId, String name, TransactionType type, boolean system) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.system = system;
    }

    public Category(String name, TransactionType type) {
        this(null, null, name, type, false);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category category)) return false;
        return Objects.equals(name != null ? name.toLowerCase() : null,
                category.name != null ? category.name.toLowerCase() : null) &&
                type == category.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name != null ? name.toLowerCase() : null, type);
    }

    @Override
    public String toString() {
        return name;
    }
}
