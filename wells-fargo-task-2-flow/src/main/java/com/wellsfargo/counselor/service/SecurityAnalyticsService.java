package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.analytics.AnalyticsCalculator;
import com.wellsfargo.counselor.dto.SecurityAnalytics;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.exception.InsufficientDataException;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Computes risk/return analytics for a security from its {@link PriceQuote} history.
 *
 * <p>Asset metrics (volatility, Sharpe, max drawdown) use the security's own series over the range.
 * When a benchmark is supplied, beta/alpha are computed on the returns over the dates the two series
 * share, so the regression uses a strictly aligned sample. Results are rounded for presentation;
 * undefined values (NaN/infinity) are surfaced as null rather than misleading numbers.
 */
@Service
@Transactional(readOnly = true)
public class SecurityAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAnalyticsService.class);
    private static final int DEFAULT_PERIODS_PER_YEAR = 252;
    private static final int MIN_PRICE_POINTS = 3; // >= 2 returns required for sample variance

    private final SecurityRepository securityRepository;
    private final PriceQuoteRepository priceQuoteRepository;

    public SecurityAnalyticsService(SecurityRepository securityRepository,
                                    PriceQuoteRepository priceQuoteRepository) {
        this.securityRepository = securityRepository;
        this.priceQuoteRepository = priceQuoteRepository;
    }

    public SecurityAnalytics analyze(Long securityId, LocalDate from, LocalDate to, Long benchmarkId,
                                     BigDecimal annualRiskFreeRate, Integer periodsPerYear) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be a date before 'to'");
        }
        int periods = (periodsPerYear != null && periodsPerYear > 0) ? periodsPerYear : DEFAULT_PERIODS_PER_YEAR;
        BigDecimal riskFree = (annualRiskFreeRate != null) ? annualRiskFreeRate : BigDecimal.ZERO;
        double riskFreeDouble = riskFree.doubleValue();

        Security security = securityRepository.findById(securityId)
                .orElseThrow(() -> ResourceNotFoundException.of("Security", securityId));

        NavigableMap<LocalDate, Double> assetSeries = priceSeries(securityId, from, to);
        if (assetSeries.size() < MIN_PRICE_POINTS) {
            throw new InsufficientDataException("At least " + MIN_PRICE_POINTS
                    + " price points are required for security " + securityId + "; found " + assetSeries.size());
        }

        double[] assetPrices = toArray(assetSeries.values());
        double[] assetReturns = AnalyticsCalculator.simpleReturns(assetPrices);
        double volatility = AnalyticsCalculator.annualizedVolatility(assetReturns, periods);
        double sharpe = AnalyticsCalculator.sharpeRatio(assetReturns, riskFreeDouble, periods);
        double maxDrawdown = AnalyticsCalculator.maxDrawdown(assetPrices);

        String benchmarkSymbol = null;
        BigDecimal beta = null;
        BigDecimal alpha = null;
        if (benchmarkId != null) {
            Security benchmark = securityRepository.findById(benchmarkId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Benchmark security", benchmarkId));
            AnalyticsCalculator.BetaAlpha betaAlpha =
                    betaAlphaAgainst(assetSeries, priceSeries(benchmarkId, from, to), riskFreeDouble, periods);
            benchmarkSymbol = benchmark.getSymbol();
            beta = round(betaAlpha.beta(), 4);
            alpha = round(betaAlpha.alpha(), 6);
        }

        log.info("Analytics security id={} range=[{},{}] obs={} vol={} sharpe={} maxDD={} benchmark={}",
                securityId, from, to, assetReturns.length, volatility, sharpe, maxDrawdown, benchmarkSymbol);

        return new SecurityAnalytics(security.getSymbol(), from, to, assetReturns.length, periods, riskFree,
                round(volatility, 6), round(sharpe, 4), round(maxDrawdown, 6), benchmarkSymbol, beta, alpha);
    }

    private AnalyticsCalculator.BetaAlpha betaAlphaAgainst(NavigableMap<LocalDate, Double> assetSeries,
                                                           NavigableMap<LocalDate, Double> benchmarkSeries,
                                                           double riskFreeDouble, int periods) {
        List<Double> alignedAsset = new ArrayList<>();
        List<Double> alignedBenchmark = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : assetSeries.entrySet()) {
            Double benchmarkPrice = benchmarkSeries.get(entry.getKey());
            if (benchmarkPrice != null) {
                alignedAsset.add(entry.getValue());
                alignedBenchmark.add(benchmarkPrice);
            }
        }
        if (alignedAsset.size() < MIN_PRICE_POINTS) {
            throw new InsufficientDataException("At least " + MIN_PRICE_POINTS
                    + " overlapping price dates are required for beta/alpha; found " + alignedAsset.size());
        }
        double[] assetReturns = AnalyticsCalculator.simpleReturns(toArray(alignedAsset));
        double[] benchmarkReturns = AnalyticsCalculator.simpleReturns(toArray(alignedBenchmark));
        return AnalyticsCalculator.betaAlpha(assetReturns, benchmarkReturns, riskFreeDouble, periods);
    }

    private NavigableMap<LocalDate, Double> priceSeries(Long securityId, LocalDate from, LocalDate to) {
        NavigableMap<LocalDate, Double> series = new TreeMap<>();
        for (PriceQuote quote : priceQuoteRepository.findBySecurityIdAndAsOfBetweenOrderByAsOfAsc(securityId, from, to)) {
            series.put(quote.getAsOf(), quote.getClose().getAmount().doubleValue());
        }
        return series;
    }

    private static double[] toArray(java.util.Collection<Double> values) {
        double[] array = new double[values.size()];
        int i = 0;
        for (double v : values) {
            array[i++] = v;
        }
        return array;
    }

    /** Rounds a statistical result for presentation; NaN/infinity become null (undefined). */
    private static BigDecimal round(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_EVEN);
    }
}
