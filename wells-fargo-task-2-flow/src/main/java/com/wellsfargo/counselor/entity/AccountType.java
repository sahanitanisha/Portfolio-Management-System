package com.wellsfargo.counselor.entity;

/**
 * Supported account products. A type-safe enum replaces the previous free-text {@code String},
 * eliminating invalid values and making the domain self-documenting. Persisted by name
 * ({@code @Enumerated(EnumType.STRING)}) so ordinal reordering can never corrupt stored data.
 */
public enum AccountType {
    CHECKING,
    SAVINGS,
    INVESTMENT,
    RETIREMENT,
    MONEY_MARKET,
    CREDIT
}
