package com.wellsfargo.counselor.exception;

/**
 * Thrown when a sell would exceed the quantity held (or no position exists). The holdings analogue of
 * {@link InsufficientFundsException}; mapped to HTTP 422.
 */
public class InsufficientHoldingsException extends RuntimeException {

    public InsufficientHoldingsException(String message) {
        super(message);
    }
}
