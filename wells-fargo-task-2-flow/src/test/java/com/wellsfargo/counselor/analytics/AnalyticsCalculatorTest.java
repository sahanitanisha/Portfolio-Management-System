package com.wellsfargo.counselor.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Unit tests for the analytics math against hand-computable values. */
class AnalyticsCalculatorTest {

    private static final double TOL = 1e-9;

    @Test
    void computesSimpleReturns() {
        double[] returns = AnalyticsCalculator.simpleReturns(new double[]{100, 120, 108});
        assertThat(returns).hasSize(2);
        assertThat(returns[0]).isCloseTo(0.20, within(TOL));
        assertThat(returns[1]).isCloseTo(-0.10, within(TOL));
    }

    @Test
    void annualizedVolatilityIsSampleStdDevTimesSqrtPeriods() {
        double[] returns = {0.20, -0.10};                  // mean 0.05, sample variance 0.045
        double expected = Math.sqrt(0.045) * Math.sqrt(252);
        assertThat(AnalyticsCalculator.annualizedVolatility(returns, 252)).isCloseTo(expected, within(TOL));
    }

    @Test
    void sharpeRatioWithZeroRiskFree() {
        double[] returns = {0.20, -0.10};                  // mean 0.05, sd sqrt(0.045)
        double expected = (0.05 / Math.sqrt(0.045)) * Math.sqrt(252);
        assertThat(AnalyticsCalculator.sharpeRatio(returns, 0.0, 252)).isCloseTo(expected, within(TOL));
    }

    @Test
    void sharpeRatioIsNaNWhenReturnsAreConstant() {
        double[] returns = {0.05, 0.05};                   // zero variance -> undefined
        assertThat(AnalyticsCalculator.sharpeRatio(returns, 0.0, 252)).isNaN();
    }

    @Test
    void maxDrawdownIsLargestPeakToTrough() {
        double[] prices = {100, 120, 90, 110, 80};         // peak 120 -> trough 80 = 40/120
        assertThat(AnalyticsCalculator.maxDrawdown(prices)).isCloseTo(40.0 / 120.0, within(TOL));
    }

    @Test
    void betaIsTwoAndAlphaZeroWhenAssetIsTwiceBenchmark() {
        double[] benchmark = {0.05, 0.10, -0.05, 0.025};
        double[] asset = {0.10, 0.20, -0.10, 0.05};        // exactly 2x benchmark
        AnalyticsCalculator.BetaAlpha result = AnalyticsCalculator.betaAlpha(asset, benchmark, 0.0, 252);
        assertThat(result.beta()).isCloseTo(2.0, within(TOL));
        assertThat(result.alpha()).isCloseTo(0.0, within(TOL));
    }

    @Test
    void betaIsNaNWhenBenchmarkHasNoVariance() {
        double[] benchmark = {0.01, 0.01, 0.01};
        double[] asset = {0.02, 0.03, 0.01};
        assertThat(AnalyticsCalculator.betaAlpha(asset, benchmark, 0.0, 252).beta()).isNaN();
    }
}
