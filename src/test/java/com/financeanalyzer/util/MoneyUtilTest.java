package com.financeanalyzer.util;

import com.financeanalyzer.model.Expense;
import com.financeanalyzer.model.Income;
import com.financeanalyzer.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoneyUtilTest {

    @Test
    void testPaiseConversion() {
        assertEquals(new BigDecimal("100.50"), MoneyUtil.toRupees(10050L));
        assertEquals(new BigDecimal("0.05"), MoneyUtil.toRupees(5L));
        assertEquals(new BigDecimal("0.00"), MoneyUtil.toRupees(0L));

        assertEquals(10050L, MoneyUtil.toPaise(new BigDecimal("100.50")));
        assertEquals(5L, MoneyUtil.toPaise(new BigDecimal("0.05")));
        assertEquals(0L, MoneyUtil.toPaise(BigDecimal.ZERO));
    }

    @Test
    void testFormatCurrency() {
        String formatted = MoneyUtil.formatCurrency(new BigDecimal("1250.50"));
        assertTrue(formatted.contains("1,250.50"));
    }

    @Test
    void testGenericSumAmountsMethod() {
        Income inc1 = new Income(1L, 1L, 1L, "Salary", new BigDecimal("5000.00"),
                LocalDate.now(), "Bank", "Salary", null, LocalDateTime.now());
        Expense exp1 = new Expense(2L, 1L, 2L, "Food", new BigDecimal("1200.00"),
                LocalDate.now(), "UPI", "Dinner", null, LocalDateTime.now());
        Expense exp2 = new Expense(3L, 1L, 3L, "Transport", new BigDecimal("300.00"),
                LocalDate.now(), "Cash", "Taxi", null, LocalDateTime.now());

        List<Transaction> list = List.of(inc1, exp1, exp2);

        // Generic sum of absolute amounts: 5000 + 1200 + 300 = 6500.00
        BigDecimal totalSum = MoneyUtil.sumAmounts(list);
        assertEquals(new BigDecimal("6500.00"), totalSum);

        // Generic sum of signed amounts: +5000 - 1200 - 300 = +3500.00
        BigDecimal signedSum = MoneyUtil.sumSignedAmounts(list);
        assertEquals(new BigDecimal("3500.00"), signedSum);
    }
}
