package com.financeanalyzer.service;

import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.dao.UserFixedCategoryDao;
import com.financeanalyzer.exception.InvalidFixedCategoryException;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.enums.TransactionType;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.financeanalyzer.util.AppLogger;

/**
 * Coordinates category queries and user-specific fixed-category preferences.
 */
public class CategoryService {

    private static final Logger LOGGER = AppLogger.getLogger(CategoryService.class);
    private final CategoryDao categoryDao;
    private final UserFixedCategoryDao userFixedCategoryDao;

    public CategoryService(CategoryDao categoryDao, UserFixedCategoryDao userFixedCategoryDao) {
        this.categoryDao = Objects.requireNonNull(categoryDao, "CategoryDao must not be null.");
        this.userFixedCategoryDao = Objects.requireNonNull(
                userFixedCategoryDao, "UserFixedCategoryDao must not be null.");
    }

    public List<Category> findExpenseCategories(long userId) {
        return categoryDao.findCategoriesForUser(userId, TransactionType.EXPENSE);
    }

    public Set<Long> findFixedCategoryIds(long userId) {
        return userFixedCategoryDao.findFixedCategoryIds(userId);
    }

    public void setFixed(long userId, long categoryId, boolean fixed)
            throws InvalidFixedCategoryException {
        validateFixedCategory(userId, categoryId);
        userFixedCategoryDao.setFixed(userId, categoryId, fixed);
        LOGGER.log(Level.INFO, "Updated fixed category for userId={0}, categoryIds={1}",
                new Object[]{userId, Set.of(categoryId)});
    }

    public void replaceAll(long userId, Set<Long> categoryIds) throws InvalidFixedCategoryException {
        Objects.requireNonNull(categoryIds, "Category IDs must not be null.");
        for (Long categoryId : categoryIds) {
            if (categoryId == null) {
                throw new InvalidFixedCategoryException("Fixed category selection cannot contain an empty category.");
            }
            validateFixedCategory(userId, categoryId);
        }
        userFixedCategoryDao.replaceAll(userId, Set.copyOf(categoryIds));
        LOGGER.log(Level.INFO, "Replaced fixed categories for userId={0}, categoryIds={1}",
                new Object[]{userId, categoryIds});
    }

    private void validateFixedCategory(long userId, long categoryId)
            throws InvalidFixedCategoryException {
        Category category = categoryDao.findById(categoryId)
                .orElseThrow(() -> new InvalidFixedCategoryException(
                        "The selected category is not available to this user."));
        boolean visible = category.getUserId() == null || category.getUserId() == userId;
        if (!visible || category.getType() != TransactionType.EXPENSE) {
            throw new InvalidFixedCategoryException(
                    "Only expense categories available to this user can be marked as fixed.");
        }
    }
}
