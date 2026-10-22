package com.financeanalyzer.exception;

/**
 * Thrown when a category cannot be marked as fixed for the specified user.
 */
public class InvalidFixedCategoryException extends AppException {

    public InvalidFixedCategoryException(String message) {
        super(message);
    }
}
