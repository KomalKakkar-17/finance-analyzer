package com.financeanalyzer.io;

import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exports transaction history to CSV format using character streams (BufferedWriter).
 * Demonstrates Unit 3 character streams.
 */
public class CsvExporter {

    private static final Logger LOGGER = AppLogger.getLogger(CsvExporter.class);
    private static final String CSV_HEADER = "Date,Type,Category,Amount,PaymentMode,Notes";

    /**
     * Writes a list of transactions to a CSV file.
     *
     * @param transactions list of transactions to export
     * @param targetFile destination CSV file
     * @return count of exported rows
     * @throws IOException on I/O error
     */
    public int exportTransactions(List<Transaction> transactions, File targetFile) throws IOException {
        Objects.requireNonNull(targetFile, "Target file cannot be null.");
        if (transactions == null || transactions.isEmpty()) {
            LOGGER.warning("No transactions provided for CSV export.");
            return 0;
        }

        LOGGER.log(Level.INFO, "Exporting {0} transactions to CSV: {1}",
                new Object[]{transactions.size(), targetFile.getAbsolutePath()});

        int writtenCount = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            writer.write(CSV_HEADER);
            writer.newLine();

            for (Transaction t : transactions) {
                if (t == null) continue;
                String line = String.format("%s,%s,%s,%s,%s,%s",
                        DateUtil.toIsoString(t.getDate()),
                        t.getType().name(),
                        escapeCsv(t.getCategoryName()),
                        t.getAmount() != null ? t.getAmount().toPlainString() : "0.00",
                        escapeCsv(t.getPaymentMode() != null ? t.getPaymentMode() : ""),
                        escapeCsv(t.getNotes() != null ? t.getNotes() : "")
                );
                writer.write(line);
                writer.newLine();
                writtenCount++;
            }
            writer.flush();
            LOGGER.log(Level.INFO, "Successfully wrote {0} transaction rows to CSV.", writtenCount);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write CSV file: " + targetFile.getName(), e);
            throw e;
        }

        return writtenCount;
    }

    private String escapeCsv(String text) {
        if (text == null) return "";
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
