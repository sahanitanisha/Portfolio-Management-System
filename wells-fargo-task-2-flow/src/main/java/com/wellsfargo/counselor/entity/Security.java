package com.wellsfargo.counselor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reference data for a tradable instrument, shared across all portfolios that hold it. The trading
 * {@code currency} and {@code symbol} are fixed at creation (write-once); descriptive fields may be
 * updated to reflect renames or corporate actions.
 */
@Entity
@Table(name = "securities", indexes = {
        @Index(name = "idx_security_symbol", columnList = "symbol", unique = true)
})
public class Security extends BaseEntity {

    @NotBlank
    @Column(name = "symbol", nullable = false, unique = true, updatable = false, length = 32)
    private String symbol;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "security_type", nullable = false)
    private SecurityType type;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO-4217 code")
    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "exchange", length = 32)
    private String exchange;

    @Size(min = 12, max = 12, message = "ISIN must be 12 characters")
    @Column(name = "isin", length = 12)
    private String isin;

    /** Required by JPA. */
    protected Security() {
    }

    public Security(String symbol, String name, SecurityType type, String currency, String exchange, String isin) {
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.exchange = exchange;
        this.isin = isin;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SecurityType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }
}
