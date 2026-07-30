package com.wellsfargo.counselor.fx;

import com.wellsfargo.counselor.entity.Money;

import java.math.BigDecimal;

/**
 * Port for currency conversion. Kept as an abstraction so the in-memory implementation used today can
 * be swapped for a live market-data feed (with caching + retry + circuit breaker) without touching the
 * valuation service.
 */
public interface ExchangeRateProvider {

    /**
     * Multiplicative rate to convert an amount in {@code fromCurrency} into {@code toCurrency}.
     * Implementations must return {@link BigDecimal#ONE} when the currencies are equal and throw
     * {@link com.wellsfargo.counselor.exception.ExchangeRateUnavailableException} when no rate is known.
     */
    BigDecimal rate(String fromCurrency, String toCurrency);

    /**
     * Converts {@code source} into {@code targetCurrency}. Same-currency conversion is a no-op, so
     * single-currency portfolios never depend on any configured rate.
     */
    default Money convert(Money source, String targetCurrency) {
        if (source.getCurrency().equals(targetCurrency)) {
            return source;
        }
        return Money.of(source.getAmount().multiply(rate(source.getCurrency(), targetCurrency)), targetCurrency);
    }
}
