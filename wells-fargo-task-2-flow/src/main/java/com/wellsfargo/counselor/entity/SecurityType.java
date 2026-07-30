package com.wellsfargo.counselor.entity;

/**
 * Asset class of a tradable {@link Security}. Persisted by name ({@code @Enumerated(EnumType.STRING)})
 * so ordinal reordering can never corrupt stored data.
 */
public enum SecurityType {
    COMMON_STOCK,
    PREFERRED_STOCK,
    ETF,
    MUTUAL_FUND,
    BOND,
    OPTION,
    FUTURE,
    REIT,
    CRYPTO,
    CASH,
    OTHER
}
