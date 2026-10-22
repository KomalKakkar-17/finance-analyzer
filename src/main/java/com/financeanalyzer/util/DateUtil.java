package com.financeanalyzer.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Utility class for date manipulation, parsing, and formatting.
 * Demonstrates Unit 2 LocalDate/YearMonth/Period utilities.
 */
public final class DateUtil {

    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    public static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DISPLAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    private DateUtil() {
        // Utility class
    }

    /**
     * Formats a LocalDate into an ISO string (yyyy-MM-dd).
     */
    public static String toIsoString(LocalDate date) {
        return date != null ? date.format(ISO_DATE_FORMATTER) : null;
    }

    /**
     * Parses an ISO string into a LocalDate.
     */
    public static LocalDate parseIsoDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), ISO_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Formats a YearMonth into 'yyyy-MM'.
     */
    public static String toMonthString(YearMonth ym) {
        return ym != null ? ym.format(MONTH_FORMATTER) : null;
    }

    /**
     * Parses a month string 'yyyy-MM' into a YearMonth.
     */
    public static YearMonth parseYearMonth(String text) {
        if (text == null || text.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(text.trim(), MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }

    /**
     * Formats a LocalDate for user-friendly UI display (e.g. "22 Oct 2026").
     */
    public static String formatDisplayDate(LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE_FORMATTER) : "";
    }

    /**
     * Formats a YearMonth for display (e.g. "October 2026").
     */
    public static String formatDisplayMonth(YearMonth ym) {
        return ym != null ? ym.format(DISPLAY_MONTH_FORMATTER) : "";
    }

    /**
     * Computes the period in months remaining between now and the target date.
     */
    public static int monthsUntil(LocalDate deadline) {
        if (deadline == null) return 0;
        LocalDate now = LocalDate.now();
        if (deadline.isBefore(now)) return 0;
        Period p = Period.between(now, deadline);
        return p.getYears() * 12 + p.getMonths() + (p.getDays() > 0 ? 1 : 0);
    }
}
