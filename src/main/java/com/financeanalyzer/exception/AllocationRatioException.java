package com.financeanalyzer.exception;

/**
 * Thrown when savings goal allocation ratios violate business rules (e.g. cumulative active ratio > 100%).
 */
public class AllocationRatioException extends AppException {

    private final int totalRatio;

    public AllocationRatioException(String message, int totalRatio) {
        super(message);
        this.totalRatio = totalRatio;
    }

    public int getTotalRatio() {
        return totalRatio;
    }
}
