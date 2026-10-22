package com.financeanalyzer.util;

import com.financeanalyzer.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for monetary computations, conversions between minor units (paise) and rupees,
 * and currency formatting.
 * Demonstrates Unit 2 Generics with bounded type parameters.
 */
public final class MoneyUtil {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    private MoneyUtil() {
        // Utility class
    }

    /**
     * Converts integer paise to BigDecimal rupees with 2 decimal scale.
     *
     * @param paise minor units
     * @return BigDecimal rupees
     */
    public static BigDecimal toRupees(long paise) {
        return BigDecimal.valueOf(paise).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /**
     * Converts BigDecimal rupees to long paise.
     *
     * @param rupees BigDecimal amount
     * @return long paise
     */
    public static long toPaise(BigDecimal rupees) {
        if (rupees == null) {
            return 0L;
        }
        return rupees.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * Formats an amount with the currency symbol from app.properties (default ₹).
     *
     * @param amount the amount to format
     * @return formatted currency string, e.g. "₹1,250.50"
     */
    public static String formatCurrency(BigDecimal amount) {
        String symbol = ConfigLoader.getProperty("currency.symbol", "\u20B9");
        if (amount == null) {
            return symbol + "0.00";
        }
        return symbol + CURRENCY_FORMAT.format(amount);
    }

    /**
     * Formats signed currency amounts, showing + for positive and - for negative.
     *
     * @param amount the amount to format
     * @return formatted string, e.g. "+₹5,000.00" or "-₹250.00"
     */
    public static String formatSignedCurrency(BigDecimal amount) {
        String symbol = ConfigLoader.getProperty("currency.symbol", "\u20B9");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return symbol + "0.00";
        }
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + symbol + CURRENCY_FORMAT.format(amount);
        } else {
            return "-" + symbol + CURRENCY_FORMAT.format(amount.abs());
        }
    }

    /**
     * Generic method demonstrating Unit 2 Generics with upper bounded wildcard.
     * Calculates the sum of absolute amounts across any collection of Transactions.
     *
     * @param transactions list of transactions extending Transaction
     * @param <T> type bounded by Transaction
     * @return total BigDecimal sum
     */
    public static <T extends Transaction> BigDecimal sumAmounts(List<T> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (T txn : transactions) {
            if (txn != null && txn.getAmount() != null) {
                sum = sum.add(txn.getAmount());
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generic method calculating net signed sum across transactions.
     *
     * @param transactions list of transactions extending Transaction
     * @param <T> type bounded by Transaction
     * @return signed net balance
     */
    public static <T extends Transaction> BigDecimal sumSignedAmounts(List<T> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal net = BigDecimal.ZERO;
        for (T txn : transactions) {
            if (txn != null) {
                net = net.add(txn.getSignedAmount());
            }
        }
        return net.setScale(2, RoundingMode.HALF_UP);
    }
}
