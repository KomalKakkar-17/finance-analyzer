package com.financeanalyzer.exception;

/**
 * Unchecked runtime exception wrapping low-level SQLExceptions from JDBC.
 * Adheres to standard enterprise DAO pattern decouplings.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
