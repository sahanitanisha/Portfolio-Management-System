package com.wellsfargo.counselor.exception;

/**
 * Thrown when there is not enough price history to compute a requested analytic (e.g. fewer than the
 * minimum number of observations, or insufficient overlap with a benchmark). Mapped to HTTP 422.
 */
public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }
}
