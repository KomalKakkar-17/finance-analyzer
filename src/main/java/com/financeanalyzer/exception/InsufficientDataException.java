package com.financeanalyzer.exception;

/**
 * Thrown when an analytical heuristic or report has insufficient historical data to run.
 */
public class InsufficientDataException extends AppException {

    public InsufficientDataException(String message) {
        super(message);
    }
}
