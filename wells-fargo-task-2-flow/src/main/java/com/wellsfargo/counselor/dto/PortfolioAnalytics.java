package com.wellsfargo.counselor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Risk/return analytics for a whole portfolio over a date range, derived from a portfolio value series
 * built from current holdings valued at historical prices. Volatility and max drawdown are fractions;
 * Sharpe/beta/alpha are dimensionless / annualised. Null when undefined.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortfolioAnalytics(
        Long portfolioId,
        String name,
        String baseCurrency,
        LocalDate from,
        LocalDate to,
        int observations,
        int periodsPerYear,
        BigDecimal riskFreeRate,
        BigDecimal annualizedVolatility,
        BigDecimal sharpeRatio,
        BigDecimal maxDrawdown,
        String benchmarkSymbol,
        BigDecimal beta,
        BigDecimal alpha
) {
}
