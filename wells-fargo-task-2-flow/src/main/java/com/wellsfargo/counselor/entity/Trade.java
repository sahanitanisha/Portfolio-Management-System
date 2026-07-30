package com.wellsfargo.counselor.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * An immutable, append-only record of an executed trade - the investment-side analogue of the cash
 * {@link Transaction} ledger. Write-once (no setters, columns {@code updatable = false}); created only
 * via the {@link #buy}/{@link #sell} factories invoked by the trade service after a {@link Position} has
 * been updated, so the ledger and the resulting position state always agree. {@code realisedPnL} is
 * populated for sells only.
 */
@Entity
@Table(name = "trades",
        uniqueConstraints = @UniqueConstraint(name = "uq_trade_reference", columnNames = "reference_number"),
        indexes = {
                @Index(name = "idx_trade_portfolio", columnList = "portfolio_id"),
                @Index(name = "idx_trade_executed_at", columnList = "executed_at")
        })
public class Trade extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false, updatable = false)
    private Portfolio portfolio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false, updatable = false)
    private Security security;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, updatable = false)
    private TradeSide side;

    @NotNull
    @Column(name = "quantity", nullable = false, updatable = false, precision = 23, scale = 6)
    private BigDecimal quantity;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "price_amount", nullable = false, updatable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "price_currency", nullable = false, updatable = false, length = 3))
    })
    private Money pricePerShare;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "realised_pnl_amount", updatable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "realised_pnl_currency", updatable = false, length = 3))
    })
    private Money realisedPnL;

    @NotNull
    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    @Column(name = "reference_number", unique = true, updatable = false, length = 64)
    private String referenceNumber;

    /** Required by JPA. */
    protected Trade() {
    }

    private Trade(Portfolio portfolio, Security security, TradeSide side, BigDecimal quantity,
                  Money pricePerShare, Money realisedPnL, String referenceNumber, Instant executedAt) {
        this.portfolio = Objects.requireNonNull(portfolio, "portfolio must not be null");
        this.security = Objects.requireNonNull(security, "security must not be null");
        this.side = Objects.requireNonNull(side, "side must not be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.pricePerShare = Objects.requireNonNull(pricePerShare, "pricePerShare must not be null");
        this.realisedPnL = realisedPnL;
        this.referenceNumber = referenceNumber;
        this.executedAt = (executedAt != null) ? executedAt : Instant.now();
    }

    public static Trade buy(Portfolio portfolio, Security security, BigDecimal quantity,
                            Money pricePerShare, String referenceNumber, Instant executedAt) {
        return new Trade(portfolio, security, TradeSide.BUY, quantity, pricePerShare, null, referenceNumber, executedAt);
    }

    public static Trade sell(Portfolio portfolio, Security security, BigDecimal quantity,
                             Money pricePerShare, Money realisedPnL, String referenceNumber, Instant executedAt) {
        return new Trade(portfolio, security, TradeSide.SELL, quantity, pricePerShare, realisedPnL, referenceNumber, executedAt);
    }

    /** Gross notional value of the trade (quantity x price), in the security's currency. */
    public Money notional() {
        return pricePerShare.multiply(quantity);
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public Security getSecurity() {
        return security;
    }

    public TradeSide getSide() {
        return side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getPricePerShare() {
        return pricePerShare;
    }

    public Money getRealisedPnL() {
        return realisedPnL;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }
}
