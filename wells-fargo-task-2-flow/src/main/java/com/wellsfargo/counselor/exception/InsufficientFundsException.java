package com.wellsfargo.counselor.exception;

/**
 * Thrown when a debit would drive an account balance negative. A domain-specific, typed exception
 * (mapped to HTTP 422) so callers and the web layer can react precisely rather than parsing a
 * generic {@link IllegalStateException} message.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
