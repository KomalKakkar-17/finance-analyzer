package com.financeanalyzer.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Centralized logging utility for the application using java.util.logging.
 * Configures both ConsoleHandler and a rotating FileHandler logging to logs/app.log.
 * Visibly demonstrates Unit 3 logging requirements.
 */
public final class AppLogger {

    private static final Logger ROOT_LOGGER = Logger.getLogger("com.financeanalyzer");
    private static FileHandler fileHandler;

    static {
        initLogger();
    }

    private AppLogger() {
        // Utility class
    }

    private static synchronized void initLogger() {
        try {
            // Ensure logs directory exists
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String logFile = ConfigLoader.getProperty("log.file", "logs/app.log");
            int limit = ConfigLoader.getIntProperty("log.file.limit", 1024 * 1024); // 1 MB
            int count = ConfigLoader.getIntProperty("log.file.count", 5);
            String levelStr = ConfigLoader.getProperty("log.level", "INFO");

            Level level;
            try {
                level = Level.parse(levelStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                level = Level.INFO;
            }

            ROOT_LOGGER.setLevel(level);
            ROOT_LOGGER.setUseParentHandlers(false);

            // Custom concise formatter
            Formatter customFormatter = new Formatter() {
                private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

                @Override
                public synchronized String format(LogRecord record) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(dateFormat.format(new Date(record.getMillis())))
                            .append(" [").append(record.getLevel().getName()).append("] ")
                            .append("[").append(Thread.currentThread().getName()).append("] ")
                            .append(record.getSourceClassName() != null ? record.getSourceClassName() : record.getLoggerName())
                            .append(" - ")
                            .append(formatMessage(record))
                            .append(System.lineSeparator());

                    if (record.getThrown() != null) {
                        java.io.StringWriter sw = new java.io.StringWriter();
                        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                        record.getThrown().printStackTrace(pw);
                        sb.append(sw.toString());
                    }
                    return sb.toString();
                }
            };

            // Console Handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(level);
            consoleHandler.setFormatter(customFormatter);
            ROOT_LOGGER.addHandler(consoleHandler);

            // Rotating File Handler
            fileHandler = new FileHandler(logFile, limit, count, true);
            fileHandler.setLevel(level);
            fileHandler.setFormatter(customFormatter);
            ROOT_LOGGER.addHandler(fileHandler);

            ROOT_LOGGER.info("AppLogger initialized successfully. Logging to: " + logFile);
        } catch (IOException e) {
            System.err.println("Failed to initialize FileHandler for AppLogger: " + e.getMessage());
        }
    }

    /**
     * Gets a logger instance namespaced for the provided class.
     *
     * @param clazz the class requesting logging
     * @return Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    /**
     * Flushes and closes all logger handlers cleanly upon application shutdown.
     */
    public static synchronized void shutdown() {
        if (fileHandler != null) {
            ROOT_LOGGER.info("Flushing and closing logger file handlers.");
            fileHandler.flush();
            fileHandler.close();
            fileHandler = null;
        }
    }
}
