package com.financeanalyzer.service;

import com.financeanalyzer.dao.AuditLogDao;
import com.financeanalyzer.dao.RecurringTransactionDao;
import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.model.AuditLog;
import com.financeanalyzer.model.Expense;
import com.financeanalyzer.model.Income;
import com.financeanalyzer.model.RecurringTransaction;
import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.enums.RuleStatus;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.ConfigLoader;
import com.financeanalyzer.util.MoneyUtil;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background scheduler handling execution of automated recurring income and expense rules.
 * Demonstrates Unit 2 explicit Iterator requirement, Unit 3 logging, and Unit 5 multi-threading (Platform.runLater).
 */
public class RecurringScheduler {

    private static final Logger LOGGER = AppLogger.getLogger(RecurringScheduler.class);

    private final RecurringTransactionDao recurringDao;
    private final TransactionDao transactionDao;
    private final AuditLogDao auditLogDao;
    private final ScheduledExecutorService scheduler;
    private Runnable uiRefreshCallback;

    public RecurringScheduler(RecurringTransactionDao recurringDao,
                              TransactionDao transactionDao,
                              AuditLogDao auditLogDao) {
        this.recurringDao = Objects.requireNonNull(recurringDao, "RecurringTransactionDao must not be null.");
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null.");
        this.auditLogDao = Objects.requireNonNull(auditLogDao, "AuditLogDao must not be null.");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RecurringScheduler-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Registers an optional callback to refresh the UI when recurring rules trigger.
     */
    public void setUiRefreshCallback(Runnable callback) {
        this.uiRefreshCallback = callback;
    }

    /**
     * Starts the scheduler with an immediate catch-up run, followed by recurring periodic checks.
     */
    public void start() {
        LOGGER.info("Starting RecurringScheduler background engine...");
        // Immediate catch-up run for any missed occurrences
        runCatchUp();

        int intervalMinutes = ConfigLoader.getIntProperty("recurring.check.interval.minutes", 5);
        scheduler.scheduleAtFixedRate(this::runCatchUp, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
        LOGGER.log(Level.INFO, "RecurringScheduler scheduled to run every {0} minutes.", intervalMinutes);
    }

    /**
     * Processes all due active rules up to today.
     * Demonstrates Unit 2 explicit Iterator usage.
     */
    public synchronized void runCatchUp() {
        LocalDate today = LocalDate.now();
        LOGGER.log(Level.INFO, "Executing recurring catch-up check for date <= {0}", today);

        try {
            List<RecurringTransaction> dueRules = recurringDao.findDueRules(today);
            LOGGER.log(Level.INFO, "Found {0} recurring rules eligible for execution.", dueRules.size());

            // Unit 2 Requirement: Visibly demonstrate an explicit Iterator
            Iterator<RecurringTransaction> iterator = dueRules.iterator();
            int executedCount = 0;

            while (iterator.hasNext()) {
                RecurringTransaction rule = iterator.next();
                try {
                    processRule(rule, today);
                    executedCount++;
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed executing recurring rule ID: " + rule.getId(), ex);
                    auditLogDao.save(new AuditLog(rule.getId(), "FAILED", "Error: " + ex.getMessage()));
                }
            }

            if (executedCount > 0) {
                LOGGER.log(Level.INFO, "Successfully processed {0} recurring rules.", executedCount);
                if (uiRefreshCallback != null) {
                    // Unit 5: Update the UI only through Platform.runLater
                    Platform.runLater(uiRefreshCallback);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in RecurringScheduler catch-up loop.", e);
        }
    }

    private void processRule(RecurringTransaction rule, LocalDate today) {
        LocalDate occurrenceDate = rule.getNextDueDate();

        // Catch up each missed occurrence until nextDueDate is in the future
        while (occurrenceDate != null && !occurrenceDate.isAfter(today)) {
            // Check if rule has expired past end date
            if (rule.getEndDate() != null && occurrenceDate.isAfter(rule.getEndDate())) {
                LOGGER.log(Level.INFO, "Recurring rule {0} reached end date {1}. Marking EXPIRED.",
                        new Object[]{rule.getId(), rule.getEndDate()});
                recurringDao.updateStatus(rule.getId(), rule.getUserId(), RuleStatus.EXPIRED);
                auditLogDao.save(new AuditLog(rule.getId(), "EXPIRED", "Rule reached end date " + rule.getEndDate()));
                break;
            }

            // Create and persist transaction for this occurrence
            Transaction txn;
            String noteText = "[Auto-Recurring] " + rule.getFrequency() + " payment";
            if (rule.getType() == TransactionType.INCOME) {
                txn = new Income(null, rule.getUserId(), rule.getCategoryId(), rule.getCategoryName(),
                        rule.getAmount(), occurrenceDate, "Auto-Debit", noteText, rule.getId(), LocalDateTime.now());
            } else {
                txn = new Expense(null, rule.getUserId(), rule.getCategoryId(), rule.getCategoryName(),
                        rule.getAmount(), occurrenceDate, "Auto-Debit", noteText, rule.getId(), LocalDateTime.now());
            }

            transactionDao.save(txn);
            LOGGER.log(Level.INFO, "Generated {0} transaction for rule ID {1}: {2} on {3}",
                    new Object[]{rule.getType(), rule.getId(), MoneyUtil.formatCurrency(rule.getAmount()), occurrenceDate});

            auditLogDao.save(new AuditLog(rule.getId(), "SUCCESS",
                    "Executed " + rule.getType() + " of " + MoneyUtil.formatCurrency(rule.getAmount()) + " for " + occurrenceDate));

            // Advance to next interval
            LocalDate nextDate = rule.getFrequency().nextDate(occurrenceDate);
            RuleStatus nextStatus = (rule.getEndDate() != null && nextDate.isAfter(rule.getEndDate()))
                    ? RuleStatus.EXPIRED
                    : RuleStatus.ACTIVE;

            recurringDao.advanceNextDueDate(rule.getId(), nextDate, nextStatus);
            rule.setNextDueDate(nextDate);
            rule.setStatus(nextStatus);

            if (nextStatus == RuleStatus.EXPIRED) {
                break;
            }

            occurrenceDate = nextDate;
        }
    }

    /**
     * Gracefully stops the background scheduler executor on application shutdown.
     */
    public void stop() {
        LOGGER.info("Shutting down RecurringScheduler background executor...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
