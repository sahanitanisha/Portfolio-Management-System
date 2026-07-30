package com.wellsfargo.counselor.exception;

/**
 * Thrown when authentication is throttled after too many failed attempts. Mapped to HTTP 429
 * (Too Many Requests) to slow credential-stuffing / brute-force attacks.
 */
public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException(String message) {
        super(message);
    }
}
