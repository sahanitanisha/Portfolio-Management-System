package com.wellsfargo.counselor.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the {@link Money} value object - the most correctness-sensitive code in the model. */
class MoneyTest {

    @Test
    void usesBankersRoundingToCurrencyScale() {
        assertThat(Money.of("10.005", "USD").getAmount()).isEqualByComparingTo("10.00"); // half to even -> down
        assertThat(Money.of("10.015", "USD").getAmount()).isEqualByComparingTo("10.02"); // half to even -> up
        assertThat(Money.of("5", "USD").getAmount().scale()).isEqualTo(2);
        assertThat(Money.of("100.5", "JPY").getAmount()).isEqualByComparingTo("100");     // JPY scale 0, to even
    }

    @Test
    void addAndSubtractAreExactAndImmutable() {
        Money base = Money.of("0.10", "USD");
        Money sum = base.add(Money.of("0.20", "USD"));
        assertThat(sum.getAmount()).isEqualByComparingTo("0.30");      // no binary-float drift
        assertThat(base.getAmount()).isEqualByComparingTo("0.10");     // operand unchanged
        assertThat(Money.of("5.00", "USD").subtract(Money.of("7.50", "USD")).isNegative()).isTrue();
    }

    @Test
    void rejectsCrossCurrencyArithmetic() {
        assertThatThrownBy(() -> Money.of("1.00", "USD").add(Money.of("1.00", "EUR")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalityIsValueBased() {
        assertThat(Money.of("3.00", "USD")).isEqualTo(Money.of("3.00", "USD"));
        assertThat(Money.of("3.00", "USD")).isNotEqualTo(Money.of("3.00", "EUR"));
    }
}
