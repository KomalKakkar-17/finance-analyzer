package com.financeanalyzer.model.enums;

import java.time.LocalDate;

/**
 * Frequency interval for recurring transactions.
 */
public enum Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * Calculates the next date based on the current date and frequency.
     *
     * @param current the current anchor date
     * @return the calculated next date
     */
    public LocalDate nextDate(LocalDate current) {
        if (current == null) {
            return LocalDate.now();
        }
        return switch (this) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case YEARLY -> current.plusYears(1);
        };
    }
}
