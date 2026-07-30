package com.wellsfargo.counselor.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An investment portfolio owned by a {@link Customer}, holding a set of {@link Position}s valued in a
 * single reporting ({@code baseCurrency}). The aggregate root for valuation and analytics.
 *
 * <p>The customer link is intentionally unidirectional (owned here) to avoid loading a potentially
 * large portfolio collection from {@code Customer}; query a customer's portfolios via the repository.
 */
@Entity
@Table(name = "portfolios")
public class Portfolio extends BaseEntity {

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "base currency must be a 3-letter ISO-4217 code")
    @Column(name = "base_currency", nullable = false, updatable = false, length = 3)
    private String baseCurrency;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Position> positions = new ArrayList<>();

    /** Required by JPA. */
    protected Portfolio() {
    }

    public Portfolio(String name, String baseCurrency, Customer customer) {
        this.name = name;
        this.baseCurrency = baseCurrency;
        this.customer = customer;
    }

    /** Adds a holding and keeps both sides of the bidirectional association in sync. */
    public void addPosition(Position position) {
        positions.add(position);
        position.setPortfolio(this);
    }

    public void removePosition(Position position) {
        positions.remove(position);
        position.setPortfolio(null);
    }

    /** Read-only view; mutate through {@link #addPosition}/{@link #removePosition}. */
    public List<Position> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public Customer getCustomer() {
        return customer;
    }
}
