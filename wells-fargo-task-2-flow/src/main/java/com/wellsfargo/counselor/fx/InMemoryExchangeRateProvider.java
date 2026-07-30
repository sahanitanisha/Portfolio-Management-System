package com.wellsfargo.counselor.fx;

import com.wellsfargo.counselor.exception.ExchangeRateUnavailableException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default {@link ExchangeRateProvider}: a thread-safe in-memory rate table. Same-currency conversions
 * need no rate; a configured direct rate is used as-is and its inverse is derived automatically; an
 * unknown pair fails fast.
 *
 * <p>This is the seam for real FX integration - replace this bean with a feed-backed provider (cached,
 * resilient) and nothing else in the valuation path changes.
 */
@Component
public class InMemoryExchangeRateProvider implements ExchangeRateProvider {

    private static final int INVERSE_SCALE = 10;

    private final ConcurrentMap<String, BigDecimal> rates = new ConcurrentHashMap<>();

    @Override
    public BigDecimal rate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        BigDecimal direct = rates.get(key(fromCurrency, toCurrency));
        if (direct != null) {
            return direct;
        }
        BigDecimal inverse = rates.get(key(toCurrency, fromCurrency));
        if (inverse != null) {
            return BigDecimal.ONE.divide(inverse, INVERSE_SCALE, RoundingMode.HALF_EVEN);
        }
        throw new ExchangeRateUnavailableException(
                "No exchange rate configured for " + fromCurrency + "->" + toCurrency);
    }

    /** Registers (or replaces) a direct conversion rate. */
    public void upsertRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        rates.put(key(fromCurrency, toCurrency), rate);
    }

    private static String key(String fromCurrency, String toCurrency) {
        return fromCurrency + "/" + toCurrency;
    }
}
