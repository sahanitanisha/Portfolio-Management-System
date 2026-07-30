package com.wellsfargo.counselor.fx;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * The {@link ExchangeRateProvider} the running application uses by default: it sources cross-currency
 * rates through the resilient {@link RemoteFxRateClient} (retry + circuit breaker). Same-currency
 * conversions are handled by the interface default and never reach the client.
 *
 * <p>Marked {@link Primary} so it wins over the plain in-memory provider in the full context; slice
 * tests that import {@link InMemoryExchangeRateProvider} directly are unaffected because this bean is
 * not present there.
 */
@Primary
@Component
public class ResilientExchangeRateProvider implements ExchangeRateProvider {

    private final RemoteFxRateClient remoteFxRateClient;

    public ResilientExchangeRateProvider(RemoteFxRateClient remoteFxRateClient) {
        this.remoteFxRateClient = remoteFxRateClient;
    }

    @Override
    public BigDecimal rate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        return remoteFxRateClient.fetchRate(fromCurrency, toCurrency);
    }
}
