package com.wellsfargo.counselor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A desired weight for a {@link Security} within a {@link Portfolio}, expressed as a fraction in
 * {@code [0, 1]}. The set of allocations for a portfolio defines its rebalancing target; weights
 * typically sum to <= 1 (any remainder is implicitly cash/unallocated). Unique per (portfolio, security).
 */
@Entity
@Table(name = "target_allocations",
        uniqueConstraints = @UniqueConstraint(name = "uq_target_portfolio_security",
                columnNames = {"portfolio_id", "security_id"}))
public class TargetAllocation extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;

    @NotNull
    @DecimalMin(value = "0.0", message = "target weight must not be negative")
    @DecimalMax(value = "1.0", message = "target weight must not exceed 1.0")
    @Column(name = "target_weight", nullable = false, precision = 9, scale = 6)
    private BigDecimal targetWeight;

    /** Required by JPA. */
    protected TargetAllocation() {
    }

    public TargetAllocation(Portfolio portfolio, Security security, BigDecimal targetWeight) {
        this.portfolio = Objects.requireNonNull(portfolio, "portfolio must not be null");
        this.security = Objects.requireNonNull(security, "security must not be null");
        this.targetWeight = Objects.requireNonNull(targetWeight, "targetWeight must not be null");
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public Security getSecurity() {
        return security;
    }

    public BigDecimal getTargetWeight() {
        return targetWeight;
    }

    public void setTargetWeight(BigDecimal targetWeight) {
        this.targetWeight = targetWeight;
    }
}
