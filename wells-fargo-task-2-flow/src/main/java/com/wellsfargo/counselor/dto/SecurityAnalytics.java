package com.wellsfargo.counselor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Risk/return analytics for a security over a date range. Volatility and max drawdown are fractions
 * (e.g. {@code 0.2245} = 22.45%); Sharpe/beta/alpha are dimensionless ratios / annualised figures.
 * Fields are null when undefined (e.g. Sharpe with zero variance, or beta/alpha without a benchmark).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SecurityAnalytics(
        String symbol,
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
