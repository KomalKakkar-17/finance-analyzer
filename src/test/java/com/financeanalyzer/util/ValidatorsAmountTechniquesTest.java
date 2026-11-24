package com.financeanalyzer.util;

import com.financeanalyzer.exception.InvalidTransactionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Second demonstration: {@link Validators#validateAmount(BigDecimal)}.
 *
 * Control flow: three decisions (null, amount &lt;= 0, scale &gt; 2) so V(G) = 4 and four
 * independent paths (WB-A1 to WB-A4). The same function is also tested with equivalence
 * partitioning (EP-A) and boundary value analysis (BV-A).
 */
class ValidatorsAmountTechniquesTest {

    // ---------------------------------------------------------------- white-box (basis paths)

    @Test
    @DisplayName("WB-A1 path 1-2: null amount")
    void wbA1_nullAmount() {
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> Validators.validateAmount(null));
        assertEquals("Amount cannot be null.", e.getMessage());
    }

    @Test
    @DisplayName("WB-A2 path 1-3-4: amount not greater than zero")
    void wbA2_nonPositive() {
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> Validators.validateAmount(new BigDecimal("-5.00")));
        assertEquals("Amount must be strictly greater than zero.", e.getMessage());
    }

    @Test
    @DisplayName("WB-A3 path 1-3-5-6: more than two decimal places")
    void wbA3_tooManyDecimals() {
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> Validators.validateAmount(new BigDecimal("10.555")));
        assertEquals("Amount cannot have more than 2 decimal places.", e.getMessage());
    }

    @Test
    @DisplayName("WB-A4 path 1-3-5-7: valid amount returns normally")
    void wbA4_valid() {
        assertDoesNotThrow(() -> Validators.validateAmount(new BigDecimal("850.00")));
    }

    // ---------------------------------------------------------------- black-box

    /** Equivalence classes (EP-A) and boundary values (BV-A) in one table: expected = OK or message key. */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "EP-A1 valid amount            | 850.00  | OK",
            "EP-A2 zero or negative        | -5.00   | NOT_POSITIVE",
            "EP-A3 more than 2 decimals    | 10.555  | TOO_MANY_DECIMALS",
            "BV-A1 just below zero         | -0.01   | NOT_POSITIVE",
            "BV-A2 zero                    | 0.00    | NOT_POSITIVE",
            "BV-A3 smallest valid amount   | 0.01    | OK",
            "BV-A4 one decimal place       | 10.5    | OK",
            "BV-A5 two decimal places      | 10.55   | OK",
            "BV-A6 three decimal places    | 10.555  | TOO_MANY_DECIMALS",
            "BV-A7 equal value, scale 3    | 100.000 | TOO_MANY_DECIMALS",
            "BV-A8 whole number            | 100     | OK"
    })
    void black_box(String id, String input, String expected) {
        BigDecimal amount = new BigDecimal(input.trim());
        switch (expected.trim()) {
            case "OK" -> assertDoesNotThrow(() -> Validators.validateAmount(amount));
            case "NOT_POSITIVE" -> assertEquals("Amount must be strictly greater than zero.",
                    assertThrows(InvalidTransactionException.class, () -> Validators.validateAmount(amount)).getMessage());
            default -> assertEquals("Amount cannot have more than 2 decimal places.",
                    assertThrows(InvalidTransactionException.class, () -> Validators.validateAmount(amount)).getMessage());
        }
    }
}
