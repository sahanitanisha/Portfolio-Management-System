package com.wellsfargo.counselor.fx;

import com.wellsfargo.counselor.exception.ExchangeRateUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stand-in for an external FX feed client, instrumented with the resilience patterns required for any
 * volatile market-data dependency: {@link Retry} with exponential backoff (rides out transient errors)
 * and a {@link CircuitBreaker} with a fallback (stops hammering a failing upstream and degrades
 * predictably). The "remote" source here is the in-memory rate table; replacing it with a real HTTP feed
 * only changes {@link #fetchRate}'s body - the resilience wiring (and the "fx" config in
 * application.properties) is unchanged.
 */
@Component
public class RemoteFxRateClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteFxRateClient.class);

    private final InMemoryExchangeRateProvider rateSource;

    public RemoteFxRateClient(InMemoryExchangeRateProvider rateSource) {
        this.rateSource = rateSource;
    }

    @Cacheable(value = "fxRates", key = "#fromCurrency + '/' + #toCurrency")
    @Retry(name = "fx")
    @CircuitBreaker(name = "fx", fallbackMethod = "rateFallback")
    public BigDecimal fetchRate(String fromCurrency, String toCurrency) {
        // A production client would perform the HTTP call to the FX provider here.
        return rateSource.rate(fromCurrency, toCurrency);
    }

    /**
     * Invoked when retries are exhausted or the circuit is open. Surfaces an explicit "unavailable"
     * signal rather than returning a stale or fabricated rate, so callers fail predictably.
     */
    @SuppressWarnings("unused")
    private BigDecimal rateFallback(String fromCurrency, String toCurrency, Throwable cause) {
        log.warn("FX rate {}->{} unavailable after resilience handling: {}",
                fromCurrency, toCurrency, cause.getMessage());
        throw new ExchangeRateUnavailableException(
                "Exchange rate " + fromCurrency + "->" + toCurrency + " is currently unavailable");
    }
}
