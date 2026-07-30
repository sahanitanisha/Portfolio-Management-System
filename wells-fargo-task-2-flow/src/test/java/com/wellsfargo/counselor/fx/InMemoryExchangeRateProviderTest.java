package com.wellsfargo.counselor.fx;

import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.exception.ExchangeRateUnavailableException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the in-memory FX provider: same-currency no-op, direct + inverse rates, fail-fast. */
class InMemoryExchangeRateProviderTest {

    @Test
    void sameCurrencyIsNoOp() {
        InMemoryExchangeRateProvider fx = new InMemoryExchangeRateProvider();
        assertThat(fx.rate("USD", "USD")).isEqualByComparingTo("1");
        Money usd = Money.of("100.00", "USD");
        assertThat(fx.convert(usd, "USD")).isSameAs(usd);
    }

    @Test
    void directRateConverts() {
        InMemoryExchangeRateProvider fx = new InMemoryExchangeRateProvider();
        fx.upsertRate("EUR", "USD", new BigDecimal("1.10"));
        assertThat(fx.convert(Money.of("100.00", "EUR"), "USD").getAmount()).isEqualByComparingTo("110.00");
    }

    @Test
    void inverseRateIsDerived() {
        InMemoryExchangeRateProvider fx = new InMemoryExchangeRateProvider();
        fx.upsertRate("EUR", "USD", new BigDecimal("1.25"));
        // USD->EUR = 1 / 1.25 = 0.8, so 100 USD -> 80 EUR
        assertThat(fx.convert(Money.of("100.00", "USD"), "EUR").getAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void missingRateFailsFast() {
        InMemoryExchangeRateProvider fx = new InMemoryExchangeRateProvider();
        assertThatThrownBy(() -> fx.convert(Money.of("100.00", "GBP"), "USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
