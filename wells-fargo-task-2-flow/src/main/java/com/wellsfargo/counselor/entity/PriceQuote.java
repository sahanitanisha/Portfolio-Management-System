package com.wellsfargo.counselor.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

/**
 * An immutable daily closing price for a {@link Security} - the time-series substrate for valuation
 * and for return/volatility/drawdown analytics.
 *
 * <p>Like the cash ledger, a quote is a historical fact: write-once (no setters, columns
 * {@code updatable = false}). The unique constraint on {@code (security_id, as_of)} guarantees one
 * bar per security per day; the composite index serves the "history between two dates" query.
 */
@Entity
@Table(name = "price_quotes",
        uniqueConstraints = @UniqueConstraint(name = "uq_quote_security_asof", columnNames = {"security_id", "as_of"}),
        indexes = @Index(name = "idx_quote_security_asof", columnList = "security_id, as_of"))
public class PriceQuote extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false, updatable = false)
    private Security security;

    @NotNull
    @Column(name = "as_of", nullable = false, updatable = false)
    private LocalDate asOf;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "close_amount", nullable = false, updatable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "close_currency", nullable = false, updatable = false, length = 3))
    })
    private Money close;

    @Column(name = "volume", updatable = false)
    private Long volume;

    /** Required by JPA. */
    protected PriceQuote() {
    }

    private PriceQuote(Security security, LocalDate asOf, Money close, Long volume) {
        this.security = Objects.requireNonNull(security, "security must not be null");
        this.asOf = Objects.requireNonNull(asOf, "asOf must not be null");
        this.close = Objects.requireNonNull(close, "close must not be null");
        this.volume = volume;
    }

    public static PriceQuote of(Security security, LocalDate asOf, Money close, Long volume) {
        return new PriceQuote(security, asOf, close, volume);
    }

    public Security getSecurity() {
        return security;
    }

    public LocalDate getAsOf() {
        return asOf;
    }

    public Money getClose() {
        return close;
    }

    public Long getVolume() {
        return volume;
    }
}
