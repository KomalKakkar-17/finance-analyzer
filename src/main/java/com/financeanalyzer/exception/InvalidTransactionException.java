package com.financeanalyzer.exception;

/**
 * Thrown when transaction data fails business or syntactic validation.
 */
public class InvalidTransactionException extends AppException {

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
