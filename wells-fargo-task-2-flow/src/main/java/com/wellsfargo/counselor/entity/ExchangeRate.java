package com.wellsfargo.counselor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * An immutable, date-indexed FX rate: the number of {@code toCurrency} units per one
 * {@code fromCurrency} unit, as of a given date. A historical fact, so it is write-once (no setters).
 * Unique per {@code (fromCurrency, toCurrency, asOf)} and indexed for an efficient "rate on or before a
 * date" lookup, which is what period-correct portfolio analytics require.
 */
@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(name = "uq_fx_pair_asof",
                columnNames = {"from_currency", "to_currency", "as_of"}),
        indexes = @Index(name = "idx_fx_pair_asof", columnList = "from_currency, to_currency, as_of"))
public class ExchangeRate extends BaseEntity {

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO-4217 code")
    @Column(name = "from_currency", nullable = false, updatable = false, length = 3)
    private String fromCurrency;

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO-4217 code")
    @Column(name = "to_currency", nullable = false, updatable = false, length = 3)
    private String toCurrency;

    @NotNull
    @Column(name = "as_of", nullable = false, updatable = false)
    private LocalDate asOf;

    @NotNull
    @Positive
    @Column(name = "rate", nullable = false, updatable = false, precision = 19, scale = 10)
    private BigDecimal rate;

    /** Required by JPA. */
    protected ExchangeRate() {
    }

    private ExchangeRate(String fromCurrency, String toCurrency, LocalDate asOf, BigDecimal rate) {
        this.fromCurrency = Objects.requireNonNull(fromCurrency, "fromCurrency must not be null");
        this.toCurrency = Objects.requireNonNull(toCurrency, "toCurrency must not be null");
        this.asOf = Objects.requireNonNull(asOf, "asOf must not be null");
        this.rate = Objects.requireNonNull(rate, "rate must not be null");
    }

    public static ExchangeRate of(String fromCurrency, String toCurrency, LocalDate asOf, BigDecimal rate) {
        return new ExchangeRate(fromCurrency, toCurrency, asOf, rate);
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public LocalDate getAsOf() {
        return asOf;
    }

    public BigDecimal getRate() {
        return rate;
    }
}
