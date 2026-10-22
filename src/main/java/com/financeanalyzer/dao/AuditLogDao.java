package com.financeanalyzer.dao;

import com.financeanalyzer.model.AuditLog;

import java.util.List;

/**
 * Data Access Object interface for recurring execution audit logs.
 */
public interface AuditLogDao extends Dao<AuditLog, Long> {

    /**
     * Finds audit records associated with a specific recurring rule.
     *
     * @param ruleId recurring rule ID
     * @return list of audit log records
     */
    List<AuditLog> findByRuleId(Long ruleId);
}
