package com.financeanalyzer.dao;

import com.financeanalyzer.model.RecurringTransaction;
import com.financeanalyzer.model.enums.RuleStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object interface for recurring transaction rules.
 */
public interface RecurringTransactionDao extends Dao<RecurringTransaction, Long> {

    /**
     * Finds all active recurring rules whose nextDueDate is on or before asOfDate.
     * Used by the background scheduler for processing due transactions.
     *
     * @param asOfDate date threshold (usually today)
     * @return list of due recurring rules
     */
    List<RecurringTransaction> findDueRules(LocalDate asOfDate);

    /**
     * Updates the status of a recurring rule (ACTIVE, PAUSED, EXPIRED).
     */
    boolean updateStatus(Long ruleId, Long userId, RuleStatus status);

    /**
     * Advances the nextDueDate of a recurring rule and updates its status if expired.
     */
    boolean advanceNextDueDate(Long ruleId, LocalDate nextDueDate, RuleStatus newStatus);
}
