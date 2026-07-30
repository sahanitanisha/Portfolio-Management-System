package com.wellsfargo.counselor.analytics;

/**
 * Generic descriptive statistics over {@code double} samples.
 *
 * <p>These operate in double-precision floating point by design: they back statistical estimates
 * (variance, standard deviation, covariance) that involve square roots and ratios, for which exact
 * decimal arithmetic is neither meaningful nor possible. Exact {@code BigDecimal} is reserved for
 * money, ledgers and valuation; statistical analytics use {@code double}.
 *
 * <p>Variance and covariance use Bessel's correction (denominator {@code n - 1}); they return
 * {@link Double#NaN} when there are fewer than two observations.
 */
public final class Statistics {

    private Statistics() {
    }

    public static double mean(double[] values) {
        if (values.length == 0) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    public static double sampleVariance(double[] values) {
        int n = values.length;
        if (n < 2) {
            return Double.NaN;
        }
        double mean = mean(values);
        double sumSquares = 0.0;
        for (double v : values) {
            double d = v - mean;
            sumSquares += d * d;
        }
        return sumSquares / (n - 1);
    }

    public static double sampleStandardDeviation(double[] values) {
        return Math.sqrt(sampleVariance(values));
    }

    public static double sampleCovariance(double[] xs, double[] ys) {
        int n = xs.length;
        if (n != ys.length || n < 2) {
            return Double.NaN;
        }
        double meanX = mean(xs);
        double meanY = mean(ys);
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += (xs[i] - meanX) * (ys[i] - meanY);
        }
        return sum / (n - 1);
    }
}
