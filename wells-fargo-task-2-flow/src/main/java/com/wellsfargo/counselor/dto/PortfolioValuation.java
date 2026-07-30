package com.wellsfargo.counselor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Point-in-time valuation of a portfolio, in its base currency.
 *
 * <p>The aggregate totals cover the <em>priced</em> positions, so the invariant
 * {@code totalUnrealisedGain == totalMarketValue - totalCostBasis} always holds. {@code unpricedCount}
 * flags when the valuation is partial (one or more holdings had no available quote); those holdings
 * still appear in {@code positions} with {@code priced=false} for transparency.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortfolioValuation(
        Long portfolioId,
        String name,
        String baseCurrency,
        Instant valuedAt,
        BigDecimal totalMarketValue,
        BigDecimal totalCostBasis,
        BigDecimal totalUnrealisedGain,
        BigDecimal totalUnrealisedGainPct,
        int positionCount,
        int pricedCount,
        int unpricedCount,
        List<PositionValuation> positions
) {
}
