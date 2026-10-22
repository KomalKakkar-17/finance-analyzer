package com.financeanalyzer.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an audit record for automated recurring rule executions.
 */
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long ruleId;
    private LocalDateTime runAt;
    private String result; // SUCCESS, SKIPPED, FAILED
    private String message;

    public AuditLog() {
        this.runAt = LocalDateTime.now();
    }

    public AuditLog(Long id, Long ruleId, LocalDateTime runAt, String result, String message) {
        this.id = id;
        this.ruleId = ruleId;
        this.runAt = runAt != null ? runAt : LocalDateTime.now();
        this.result = result;
        this.message = message;
    }

    public AuditLog(Long ruleId, String result, String message) {
        this(null, ruleId, LocalDateTime.now(), result, message);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public LocalDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt) {
        this.runAt = runAt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", ruleId=" + ruleId +
                ", runAt=" + runAt +
                ", result='" + result + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
