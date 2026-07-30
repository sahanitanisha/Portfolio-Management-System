package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.analytics.AnalyticsCalculator;
import com.wellsfargo.counselor.dto.PortfolioAnalytics;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.Trade;
import com.wellsfargo.counselor.entity.TradeSide;
import com.wellsfargo.counselor.exception.InsufficientDataException;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import com.wellsfargo.counselor.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Portfolio-level risk/return analytics, computed from a <em>trade-aware</em> portfolio value series.
 *
 * <p>Holdings are reconstructed as of each historical date by replaying the immutable {@link Trade}
 * ledger (cumulative buys minus sells with an execution date on or before that day), then valued at
 * that day's prices. This reflects what the portfolio actually held over time - unlike a current-weights
 * approximation. Only dates on which every then-held security has a price are used, so each point is a
 * complete valuation; dates with zero net value are excluded to keep returns well-defined. Prices are
 * converted to the base currency via {@link ExchangeRateProvider}. The resulting series feeds the shared
 * {@link AnalyticsCalculator}.
 */
@Service
@Transactional(readOnly = true)
public class PortfolioAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAnalyticsService.class);
    private static final int DEFAULT_PERIODS_PER_YEAR = 252;
    private static final int MIN_POINTS = 3;

    private final PortfolioRepository portfolioRepository;
    private final TradeRepository tradeRepository;
    private final PriceQuoteRepository priceQuoteRepository;
    private final SecurityRepository securityRepository;
    private final HistoricalExchangeRateService historicalExchangeRateService;

    public PortfolioAnalyticsService(PortfolioRepository portfolioRepository, TradeRepository tradeRepository,
                                     PriceQuoteRepository priceQuoteRepository, SecurityRepository securityRepository,
                                     HistoricalExchangeRateService historicalExchangeRateService) {
        this.portfolioRepository = portfolioRepository;
        this.tradeRepository = tradeRepository;
        this.priceQuoteRepository = priceQuoteRepository;
        this.securityRepository = securityRepository;
        this.historicalExchangeRateService = historicalExchangeRateService;
    }

    public PortfolioAnalytics analyze(Long portfolioId, LocalDate from, LocalDate to, Long benchmarkId,
                                      BigDecimal annualRiskFreeRate, Integer periodsPerYear) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be a date before 'to'");
        }
        int periods = (periodsPerYear != null && periodsPerYear > 0) ? periodsPerYear : DEFAULT_PERIODS_PER_YEAR;
        BigDecimal riskFree = (annualRiskFreeRate != null) ? annualRiskFreeRate : BigDecimal.ZERO;
        double riskFreeDouble = riskFree.doubleValue();

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> ResourceNotFoundException.of("Portfolio", portfolioId));
        String base = portfolio.getBaseCurrency();

        List<Trade> trades = tradeRepository.findByPortfolioIdOrderByExecutedAtAsc(portfolioId);
        if (trades.isEmpty()) {
            throw new InsufficientDataException("Portfolio " + portfolioId + " has no trades to analyse");
        }

        NavigableMap<LocalDate, Double> valueSeries = reconstructValueSeries(trades, base, from, to);
        if (valueSeries.size() < MIN_POINTS) {
            throw new InsufficientDataException("At least " + MIN_POINTS
                    + " complete valuation dates are required; found " + valueSeries.size());
        }

        double[] values = toArray(valueSeries.values());
        double[] returns = AnalyticsCalculator.simpleReturns(values);
        double volatility = AnalyticsCalculator.annualizedVolatility(returns, periods);
        double sharpe = AnalyticsCalculator.sharpeRatio(returns, riskFreeDouble, periods);
        double maxDrawdown = AnalyticsCalculator.maxDrawdown(values);

        String benchmarkSymbol = null;
        BigDecimal beta = null;
        BigDecimal alpha = null;
        if (benchmarkId != null) {
            Security benchmark = securityRepository.findById(benchmarkId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Benchmark security", benchmarkId));
            AnalyticsCalculator.BetaAlpha betaAlpha = betaAlphaAgainst(
                    valueSeries, basePriceSeries(benchmarkId, base, from, to), riskFreeDouble, periods);
            benchmarkSymbol = benchmark.getSymbol();
            beta = round(betaAlpha.beta(), 4);
            alpha = round(betaAlpha.alpha(), 6);
        }

        log.info("Portfolio analytics id={} range=[{},{}] obs={} vol={} sharpe={} maxDD={} benchmark={}",
                portfolioId, from, to, returns.length, volatility, sharpe, maxDrawdown, benchmarkSymbol);

        return new PortfolioAnalytics(portfolioId, portfolio.getName(), base, from, to, returns.length, periods,
                riskFree, round(volatility, 6), round(sharpe, 4), round(maxDrawdown, 6), benchmarkSymbol, beta, alpha);
    }

    /**
     * Replays the trade ledger to value the portfolio on each candidate date. Holdings on a date are the
     * cumulative signed quantity of trades executed on or before it; a date is included only if every
     * non-zero holding has a price and the total value is positive.
     */
    private NavigableMap<LocalDate, Double> reconstructValueSeries(List<Trade> trades, String base,
                                                                   LocalDate from, LocalDate to) {
        Map<Long, NavigableMap<LocalDate, Double>> priceSeriesBySecurity = new LinkedHashMap<>();
        for (Trade trade : trades) {
            Long securityId = trade.getSecurity().getId();
            priceSeriesBySecurity.computeIfAbsent(securityId, id -> basePriceSeries(id, base, from, to));
        }

        TreeSet<LocalDate> candidateDates = new TreeSet<>();
        priceSeriesBySecurity.values().forEach(series -> candidateDates.addAll(series.keySet()));

        NavigableMap<LocalDate, Double> valueByDate = new TreeMap<>();
        Map<Long, BigDecimal> holdings = new HashMap<>();
        int tradeIndex = 0;
        for (LocalDate date : candidateDates) {
            while (tradeIndex < trades.size() && executionDate(trades.get(tradeIndex)).compareTo(date) <= 0) {
                Trade trade = trades.get(tradeIndex++);
                BigDecimal signed = (trade.getSide() == TradeSide.BUY)
                        ? trade.getQuantity() : trade.getQuantity().negate();
                holdings.merge(trade.getSecurity().getId(), signed, BigDecimal::add);
            }
            double value = 0.0;
            boolean complete = true;
            for (Map.Entry<Long, BigDecimal> holding : holdings.entrySet()) {
                if (holding.getValue().signum() == 0) {
                    continue;
                }
                Double price = priceSeriesBySecurity.get(holding.getKey()).get(date);
                if (price == null) {
                    complete = false;
                    break;
                }
                value += holding.getValue().doubleValue() * price;
            }
            if (complete && value > 0.0) {
                valueByDate.put(date, value);
            }
        }
        return valueByDate;
    }

    private AnalyticsCalculator.BetaAlpha betaAlphaAgainst(NavigableMap<LocalDate, Double> portfolioValues,
                                                           NavigableMap<LocalDate, Double> benchmarkSeries,
                                                           double riskFreeDouble, int periods) {
        List<Double> alignedPortfolio = new ArrayList<>();
        List<Double> alignedBenchmark = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : portfolioValues.entrySet()) {
            Double benchmarkPrice = benchmarkSeries.get(entry.getKey());
            if (benchmarkPrice != null) {
                alignedPortfolio.add(entry.getValue());
                alignedBenchmark.add(benchmarkPrice);
            }
        }
        if (alignedPortfolio.size() < MIN_POINTS) {
            throw new InsufficientDataException("At least " + MIN_POINTS
                    + " overlapping dates with the benchmark are required; found " + alignedPortfolio.size());
        }
        double[] portfolioReturns = AnalyticsCalculator.simpleReturns(toArray(alignedPortfolio));
        double[] benchmarkReturns = AnalyticsCalculator.simpleReturns(toArray(alignedBenchmark));
        return AnalyticsCalculator.betaAlpha(portfolioReturns, benchmarkReturns, riskFreeDouble, periods);
    }

    private NavigableMap<LocalDate, Double> basePriceSeries(Long securityId, String base, LocalDate from, LocalDate to) {
        NavigableMap<LocalDate, Double> series = new TreeMap<>();
        for (PriceQuote quote : priceQuoteRepository.findBySecurityIdAndAsOfBetweenOrderByAsOfAsc(securityId, from, to)) {
            double priceBase = historicalExchangeRateService.convert(quote.getClose(), base, quote.getAsOf())
                    .getAmount().doubleValue();
            series.put(quote.getAsOf(), priceBase);
        }
        return series;
    }

    private static LocalDate executionDate(Trade trade) {
        return trade.getExecutedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static double[] toArray(Collection<Double> values) {
        double[] array = new double[values.size()];
        int i = 0;
        for (double v : values) {
            array[i++] = v;
        }
        return array;
    }

    private static BigDecimal round(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_EVEN);
    }
}
