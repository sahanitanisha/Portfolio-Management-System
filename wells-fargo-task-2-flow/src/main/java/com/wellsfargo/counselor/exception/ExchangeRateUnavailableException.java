package com.wellsfargo.counselor.exception;

/**
 * Thrown when a currency conversion is required but no exchange rate is available for the pair.
 * Mapped to HTTP 422. Failing fast here is deliberate: a portfolio valuation must never silently
 * fabricate or drop value because a rate was missing.
 */
public class ExchangeRateUnavailableException extends RuntimeException {

    public ExchangeRateUnavailableException(String message) {
        super(message);
    }
}
