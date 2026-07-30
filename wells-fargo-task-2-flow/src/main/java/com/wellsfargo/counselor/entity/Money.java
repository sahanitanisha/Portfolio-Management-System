package com.wellsfargo.counselor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable, currency-aware monetary value object.
 *
 * <p>Monetary amounts are represented as {@link BigDecimal} (never {@code double}/{@code float},
 * which cannot represent decimal cents exactly) paired with an ISO-4217 currency. All arithmetic is
 * currency-checked and every result is normalised to the currency's default fraction digits using
 * banker's rounding ({@link RoundingMode#HALF_EVEN}), the convention used for financial settlement
 * because it is statistically unbiased over many roundings.
 *
 * <p>Modelled as a JPA {@link Embeddable} so each owner maps it to its own columns via
 * {@code @AttributeOverrides}; this keeps currency travelling with the amount it qualifies and makes
 * a mismatched-currency operation impossible to express.
 */
@Embeddable
public final class Money implements Serializable {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** ISO-4217 alphabetic currency code (e.g. {@code "USD"}). */
    @NotNull
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Required by JPA. Not for application use. */
    protected Money() {
    }

    private Money(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        this.currency = currency.getCurrencyCode();
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), ROUNDING);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(String currencyCode) {
        return new Money(BigDecimal.ZERO, Currency.getInstance(currencyCode));
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), Currency.getInstance(this.currency));
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), Currency.getInstance(this.currency));
    }

    /**
     * Scales this amount by a dimensionless factor (e.g. a share quantity or an allocation weight),
     * re-normalising to the currency's scale with banker's rounding. Used for valuation
     * (quantity x price) and proportional cost-basis math.
     */
    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Money(this.amount.multiply(factor), Currency.getInstance(this.currency));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "operand must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(this.currency, other.currency));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        // Scale is normalised at construction, so exact equality on amount is well-defined here.
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
