package com.wellsfargo.counselor.analytics;

/**
 * Pure financial-analytics functions over price/return series. Stateless and side-effect free, so they
 * are trivially unit-testable and reusable. Degenerate inputs (zero variance, mismatched lengths) yield
 * {@link Double#NaN} rather than throwing, leaving presentation policy to the caller.
 *
 * <p>Conventions: simple (arithmetic) returns; sample statistics (n-1); volatility and Sharpe are
 * annualised by {@code sqrt(periodsPerYear)} (use 252 for daily data); risk-free input is an annual
 * rate, converted to per-period internally.
 */
public final class AnalyticsCalculator {

    private AnalyticsCalculator() {
    }

    /** Simple period returns: r[i] = price[i] / price[i-1] - 1. Empty when fewer than two prices. */
    public static double[] simpleReturns(double[] prices) {
        if (prices.length < 2) {
            return new double[0];
        }
        double[] returns = new double[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = prices[i] / prices[i - 1] - 1.0;
        }
        return returns;
    }

    /** Annualised volatility = sample standard deviation of returns x sqrt(periodsPerYear). */
    public static double annualizedVolatility(double[] returns, int periodsPerYear) {
        return Statistics.sampleStandardDeviation(returns) * Math.sqrt(periodsPerYear);
    }

    /** Annualised Sharpe ratio; NaN when the return standard deviation is zero (undefined). */
    public static double sharpeRatio(double[] returns, double annualRiskFreeRate, int periodsPerYear) {
        double stdDev = Statistics.sampleStandardDeviation(returns);
        if (stdDev == 0.0 || Double.isNaN(stdDev)) {
            return Double.NaN;
        }
        double riskFreePerPeriod = annualRiskFreeRate / periodsPerYear;
        double meanExcessReturn = Statistics.mean(returns) - riskFreePerPeriod;
        return (meanExcessReturn / stdDev) * Math.sqrt(periodsPerYear);
    }

    /** Maximum peak-to-trough decline over the price series, as a non-negative fraction. */
    public static double maxDrawdown(double[] prices) {
        if (prices.length == 0) {
            return 0.0;
        }
        double peak = prices[0];
        double maxDrawdown = 0.0;
        for (double price : prices) {
            if (price > peak) {
                peak = price;
            }
            if (peak > 0.0) {
                double drawdown = (peak - price) / peak;
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
        }
        return maxDrawdown;
    }

    /**
     * Market beta and (annualised Jensen's) alpha of an asset versus a benchmark, computed over aligned
     * return series of equal length. Beta = cov(asset, benchmark) / var(benchmark); alpha = mean(asset)
     * - [rf + beta x (mean(benchmark) - rf)]. Returns NaN beta/alpha when benchmark variance is zero.
     */
    public static BetaAlpha betaAlpha(double[] assetReturns, double[] benchmarkReturns,
                                      double annualRiskFreeRate, int periodsPerYear) {
        double benchmarkVariance = Statistics.sampleVariance(benchmarkReturns);
        if (benchmarkVariance == 0.0 || Double.isNaN(benchmarkVariance)) {
            return new BetaAlpha(Double.NaN, Double.NaN);
        }
        double beta = Statistics.sampleCovariance(assetReturns, benchmarkReturns) / benchmarkVariance;
        double riskFreePerPeriod = annualRiskFreeRate / periodsPerYear;
        double alphaPerPeriod = Statistics.mean(assetReturns)
                - (riskFreePerPeriod + beta * (Statistics.mean(benchmarkReturns) - riskFreePerPeriod));
        return new BetaAlpha(beta, alphaPerPeriod * periodsPerYear);
    }

    /** Beta and annualised alpha pair. */
    public record BetaAlpha(double beta, double alpha) {
    }
}
