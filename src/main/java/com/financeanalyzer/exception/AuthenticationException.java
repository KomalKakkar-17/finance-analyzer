package com.financeanalyzer.exception;

/**
 * Checked exception thrown upon failed authentication or registration validation.
 */
public class AuthenticationException extends AppException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
