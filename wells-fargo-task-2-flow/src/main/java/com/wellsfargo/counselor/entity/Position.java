package com.wellsfargo.counselor.entity;

import com.wellsfargo.counselor.exception.InsufficientHoldingsException;
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
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * A holding of one {@link Security} within one {@link Portfolio}, tracked by the average-cost method.
 *
 * <p>{@code costBasis} is the total amount invested in the <em>current</em> holding (a real money
 * amount, so {@link Money} is correct); per-share average is derived on demand at higher precision to
 * avoid rounding drift across many trades. Quantity supports fractional shares. The unique constraint
 * on {@code (portfolio_id, security_id)} keeps one aggregated position per security per portfolio.
 *
 * <p>{@code @Version} (inherited) guards concurrent trade application; the future trade service will
 * load this under a pessimistic lock and call {@link #applyBuy}/{@link #applySell}.
 */
@Entity
@Table(name = "positions",
        uniqueConstraints = @UniqueConstraint(name = "uq_position_portfolio_security",
                columnNames = {"portfolio_id", "security_id"}),
        indexes = @Index(name = "idx_position_portfolio", columnList = "portfolio_id"))
public class Position extends BaseEntity {

    private static final int RATIO_SCALE = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;

    @NotNull
    @PositiveOrZero
    @Column(name = "quantity", nullable = false, precision = 23, scale = 6)
    private BigDecimal quantity;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "cost_basis_amount", nullable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "cost_basis_currency", nullable = false, length = 3))
    })
    private Money costBasis;

    @NotNull
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    /** Required by JPA. */
    protected Position() {
    }

    public Position(Portfolio portfolio, Security security, BigDecimal quantity, Money costBasis) {
        this.portfolio = Objects.requireNonNull(portfolio, "portfolio must not be null");
        this.security = Objects.requireNonNull(security, "security must not be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.costBasis = Objects.requireNonNull(costBasis, "costBasis must not be null");
        this.openedAt = Instant.now();
    }

    /** Increases the holding and rolls the cost basis forward (average-cost method). */
    public void applyBuy(BigDecimal qty, Money pricePerShare) {
        requirePositive(qty);
        this.costBasis = this.costBasis.add(pricePerShare.multiply(qty));
        this.quantity = this.quantity.add(qty);
    }

    /**
     * Reduces the holding and returns the realised profit/loss. Cost basis is removed in proportion
     * to the fraction sold; selling more than is held is rejected.
     */
    public Money applySell(BigDecimal qty, Money pricePerShare) {
        requirePositive(qty);
        if (qty.compareTo(this.quantity) > 0) {
            throw new InsufficientHoldingsException("Cannot sell " + qty + " of " + security.getSymbol()
                    + "; only " + this.quantity + " held");
        }
        BigDecimal fraction = qty.divide(this.quantity, RATIO_SCALE, ROUNDING);
        Money costRemoved = this.costBasis.multiply(fraction);
        Money proceeds = pricePerShare.multiply(qty);
        Money realised = proceeds.subtract(costRemoved);

        this.quantity = this.quantity.subtract(qty);
        this.costBasis = this.quantity.signum() == 0
                ? Money.zero(this.costBasis.getCurrency())
                : this.costBasis.subtract(costRemoved);
        return realised;
    }

    /** Market value of the holding at the given price (quantity x price). */
    public Money marketValue(Money pricePerShare) {
        return pricePerShare.multiply(this.quantity);
    }

    /** Unrealised gain/loss at the given price (market value - cost basis). */
    public Money unrealisedGain(Money pricePerShare) {
        return marketValue(pricePerShare).subtract(this.costBasis);
    }

    /** Derived average cost per share at higher precision; zero for a closed position. */
    public BigDecimal averageCostPerShare() {
        if (this.quantity.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return this.costBasis.getAmount().divide(this.quantity, 6, ROUNDING);
    }

    private static void requirePositive(BigDecimal qty) {
        Objects.requireNonNull(qty, "quantity must not be null");
        if (qty.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    /** Package-private: managed by {@link Portfolio#addPosition}/{@link Portfolio#removePosition}. */
    void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public Security getSecurity() {
        return security;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Money getCostBasis() {
        return costBasis;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }
}
