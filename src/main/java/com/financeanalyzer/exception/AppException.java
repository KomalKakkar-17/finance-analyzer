package com.financeanalyzer.exception;

/**
 * Base checked exception class for all custom application-specific exceptions.
 * Demonstrates Unit 2 exception hierarchy design.
 */
public class AppException extends Exception {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
