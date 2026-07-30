package com.wellsfargo.counselor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wellsfargo.counselor.entity.SecurityType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Valuation of a single holding. Monetary figures ({@code marketValue}, {@code costBasis},
 * {@code unrealisedGain}) are in the portfolio's base currency; {@code lastPrice} is the security's
 * native price. When {@code priced} is false (no quote available) the market-value-derived fields are null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PositionValuation(
        Long positionId,
        String symbol,
        String name,
        SecurityType securityType,
        BigDecimal quantity,
        String currency,
        BigDecimal lastPrice,
        LocalDate priceAsOf,
        boolean priced,
        BigDecimal marketValue,
        BigDecimal costBasis,
        BigDecimal unrealisedGain,
        BigDecimal unrealisedGainPct
) {
}
