package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.User;
import com.financeanalyzer.model.enums.TransactionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcUserFixedCategoryDaoTest {

    private static JdbcUserFixedCategoryDao fixedCategoryDao;
    private static JdbcCategoryDao categoryDao;
    private static JdbcUserDao userDao;

    @BeforeAll
    static void initializeDatabase() {
        DatabaseManager.initializeDatabase();
        fixedCategoryDao = new JdbcUserFixedCategoryDao();
        categoryDao = new JdbcCategoryDao();
        userDao = new JdbcUserDao();
    }

    @Test
    void replaceAllRollsBackEntireUpdateWhenAnyCategoryIsInvalid() {
        long userId = createUser();
        List<Category> categories = categoryDao.findCategoriesForUser(userId, TransactionType.EXPENSE);
        assertTrue(categories.size() >= 2);
        long originalCategoryId = categories.get(0).getId();
        long replacementCategoryId = categories.get(1).getId();

        fixedCategoryDao.replaceAll(userId, Set.of(originalCategoryId));

        assertThrows(DataAccessException.class,
                () -> fixedCategoryDao.replaceAll(userId, Set.of(replacementCategoryId, Long.MAX_VALUE)));

        assertEquals(Set.of(originalCategoryId), fixedCategoryDao.findFixedCategoryIds(userId));
    }

    @Test
    void fixedCategoryListsAreIsolatedPerUser() {
        long firstUserId = createUser();
        long secondUserId = createUser();
        long expenseCategoryId = categoryDao.findCategoriesForUser(firstUserId, TransactionType.EXPENSE)
                .get(0).getId();

        fixedCategoryDao.replaceAll(firstUserId, Set.of(expenseCategoryId));
        fixedCategoryDao.replaceAll(secondUserId, Set.of());

        assertEquals(Set.of(expenseCategoryId), fixedCategoryDao.findFixedCategoryIds(firstUserId));
        assertTrue(fixedCategoryDao.findFixedCategoryIds(secondUserId).isEmpty());
    }

    private static long createUser() {
        String username = "fixed_test_" + UUID.randomUUID().toString().replace("-", "");
        User user = userDao.save(new User(username, "test-password-hash"));
        return user.getId();
    }
}
