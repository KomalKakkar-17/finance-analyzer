package com.financeanalyzer.util;

import com.financeanalyzer.exception.InvalidTransactionException;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ValidatorsTest {

    @Test
    void testUsernameValidation() {
        assertTrue(Validators.isValidUsername("validUser123"));
        assertTrue(Validators.isValidUsername("john_doe-99"));
        assertTrue(Validators.isValidUsername("demo"));

        // Invalid cases: less than 6 chars (except pre-seeded demo), null, invalid characters
        assertFalse(Validators.isValidUsername(null));
        assertFalse(Validators.isValidUsername(""));
        assertFalse(Validators.isValidUsername("short"));
        assertFalse(Validators.isValidUsername("user with spaces"));
        assertFalse(Validators.isValidUsername("user@bad!"));
    }

    @Test
    void testPasswordValidation() {
        assertTrue(Validators.isValidPassword("secret123"));
        assertFalse(Validators.isValidPassword(null));
        assertFalse(Validators.isValidPassword("12345")); // 5 chars
    }

    @Test
    void testAmountValidation() {
        assertDoesNotThrow(() -> Validators.validateAmount(new BigDecimal("100.00")));
        assertDoesNotThrow(() -> Validators.validateAmount(new BigDecimal("0.50")));

        // Zero or negative
        assertThrows(InvalidTransactionException.class, () -> Validators.validateAmount(BigDecimal.ZERO));
        assertThrows(InvalidTransactionException.class, () -> Validators.validateAmount(new BigDecimal("-5.00")));

        // More than 2 decimal places
        assertThrows(InvalidTransactionException.class, () -> Validators.validateAmount(new BigDecimal("10.555")));
    }

    @Test
    void testExpenseDateValidation() {
        Category cat = new Category(1L, 1L, "Food", TransactionType.EXPENSE, true);

        // Valid: today or past
        assertDoesNotThrow(() -> Validators.validateExpense(new BigDecimal("100.00"), LocalDate.now(), cat));
        assertDoesNotThrow(() -> Validators.validateExpense(new BigDecimal("100.00"), LocalDate.now().minusDays(5), cat));

        // Invalid: future date
        assertThrows(InvalidTransactionException.class, () ->
                Validators.validateExpense(new BigDecimal("100.00"), LocalDate.now().plusDays(1), cat)
        );
    }
}
