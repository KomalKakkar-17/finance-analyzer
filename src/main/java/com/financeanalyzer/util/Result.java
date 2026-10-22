package com.financeanalyzer.util;

import java.util.Objects;
import java.util.Optional;

/**
 * Generic container representing either a successful result with data, or an error state.
 * Visibly demonstrates Unit 2 Generics and functional error handling.
 *
 * @param <T> payload type
 */
public final class Result<T> {

    private final T data;
    private final String errorMessage;
    private final boolean success;

    private Result(T data, String errorMessage, boolean success) {
        this.data = data;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, true);
    }

    public static <T> Result<T> failure(String errorMessage) {
        return new Result<>(null, Objects.requireNonNullElse(errorMessage, "Unknown error"), false);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public Optional<T> getData() {
        return Optional.ofNullable(data);
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
