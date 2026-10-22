package com.financeanalyzer.dao;

import java.util.Set;

/**
 * Data access contract for a user's fixed expense categories.
 */
public interface UserFixedCategoryDao {

    Set<Long> findFixedCategoryIds(long userId);

    void setFixed(long userId, long categoryId, boolean fixed);

    void replaceAll(long userId, Set<Long> categoryIds);

    /**
     * Seeds configured defaults once for a user who has not initialized this preference.
     */
    void initializeDefaultsIfNeeded(long userId);
}
