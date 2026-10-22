package com.financeanalyzer.io;

import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.service.TransactionService;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.Validators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Imports transactions from CSV format using character streams (BufferedReader).
 * Features per-line robust error handling and logs/reports skipped lines.
 * Visibly demonstrates Unit 3 Character streams and fault-tolerant parsing.
 */
public class CsvImporter {

    private static final Logger LOGGER = AppLogger.getLogger(CsvImporter.class);

    private final TransactionService transactionService;
    private final CategoryDao categoryDao;

    public CsvImporter(TransactionService transactionService, CategoryDao categoryDao) {
        this.transactionService = Objects.requireNonNull(transactionService, "TransactionService must not be null.");
        this.categoryDao = Objects.requireNonNull(categoryDao, "CategoryDao must not be null.");
    }

    /**
     * Parses and imports transactions from a CSV file.
     *
     * @param userId owner user ID
     * @param csvFile source CSV file
     * @return ImportSummary detailing success and skipped lines
     * @throws IOException on file read error
     */
    public ImportSummary importTransactions(Long userId, File csvFile) throws IOException {
        Objects.requireNonNull(csvFile, "CSV file cannot be null.");
        LOGGER.log(Level.INFO, "Beginning CSV import from {0} for user ID {1}",
                new Object[]{csvFile.getAbsolutePath(), userId});

        int importedCount = 0;
        List<SkippedLineReport> skippedReports = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.toLowerCase().contains("date")) {
                    continue; // Skip header row
                }
                if (line.trim().isEmpty()) {
                    continue; // Skip empty rows
                }

                // Per-line error isolation
                try {
                    processLine(userId, line, lineNumber);
                    importedCount++;
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Skipped line {0}: {1} | Reason: {2}",
                            new Object[]{lineNumber, line, ex.getMessage()});
                    skippedReports.add(new SkippedLineReport(lineNumber, line, ex.getMessage()));
                }
            }
        }

        LOGGER.log(Level.INFO, "CSV import completed. Successfully imported: {0}, Skipped: {1}",
                new Object[]{importedCount, skippedReports.size()});

        return new ImportSummary(importedCount, skippedReports);
    }

    private void processLine(Long userId, String line, int lineNumber) throws Exception {
        String[] tokens = parseCsvLine(line);
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Expected at least 4 columns (Date, Type, Category, Amount)");
        }

        // 1. Date
        LocalDate date = DateUtil.parseIsoDate(tokens[0]);
        if (date == null) {
            throw new IllegalArgumentException("Invalid date format: '" + tokens[0] + "'. Expected yyyy-MM-dd");
        }

        // 2. Type
        TransactionType type;
        try {
            type = TransactionType.valueOf(tokens[1].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction type: '" + tokens[1] + "'. Must be INCOME or EXPENSE");
        }

        // 3. Category
        String catName = tokens[2].trim();
        if (catName.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        Category category = categoryDao.findByName(userId, catName, type)
                .orElseGet(() -> {
                    try {
                        return transactionService.addCustomCategory(userId, catName, type);
                    } catch (Exception e) {
                        return categoryDao.findCategoriesForUser(userId, type).get(0);
                    }
                });

        // 4. Amount
        BigDecimal amount;
        try {
            amount = new BigDecimal(tokens[3].trim());
            Validators.validateAmount(amount);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid monetary amount: '" + tokens[3] + "'");
        }

        // 5. Payment Mode & Notes
        String paymentMode = tokens.length > 4 ? tokens[4].trim() : "Other";
        String notes = tokens.length > 5 ? tokens[5].trim() : "";

        if (type == TransactionType.INCOME) {
            transactionService.addIncome(userId, category.getId(), amount, date, paymentMode, notes);
        } else {
            transactionService.addExpense(userId, category.getId(), amount, date, paymentMode, notes);
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString().trim());
        return list.toArray(new String[0]);
    }

    /**
     * Report record for a skipped line.
     */
    public record SkippedLineReport(int lineNumber, String rawLine, String reason) {}

    /**
     * Summary container for CSV import results.
     */
    public record ImportSummary(int importedCount, List<SkippedLineReport> skippedReports) {
        public int getSkippedCount() {
            return skippedReports.size();
        }
    }
}
