package com.financeanalyzer.io;

import com.financeanalyzer.util.AppLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Character stream writer writing generated monthly reports to .txt format.
 * Visibly demonstrates Unit 3 Character streams (BufferedWriter).
 */
public class ReportWriter {

    private static final Logger LOGGER = AppLogger.getLogger(ReportWriter.class);

    /**
     * Writes formatted report content to a target text file.
     *
     * @param reportContent plain text report
     * @param targetFile destination .txt file
     * @throws IOException on file write error
     */
    public void writeReport(String reportContent, File targetFile) throws IOException {
        Objects.requireNonNull(reportContent, "Report content cannot be null.");
        Objects.requireNonNull(targetFile, "Target file cannot be null.");

        LOGGER.log(Level.INFO, "Writing monthly report to file: {0}", targetFile.getAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            writer.write(reportContent);
            writer.flush();
            LOGGER.log(Level.INFO, "Successfully wrote report file: {0}", targetFile.getName());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed writing report to " + targetFile.getName(), e);
            throw e;
        }
    }
}
