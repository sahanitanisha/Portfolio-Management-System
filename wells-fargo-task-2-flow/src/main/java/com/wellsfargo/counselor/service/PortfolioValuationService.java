package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.PortfolioValuation;
import com.wellsfargo.counselor.dto.PositionValuation;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.Position;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.fx.ExchangeRateProvider;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import com.wellsfargo.counselor.repository.PositionRepository;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Computes point-in-time portfolio valuations: per-position and aggregate market value, cost basis and
 * unrealised P&amp;L, expressed in the portfolio's base currency.
 *
 * <p>Each holding is valued at its security's latest available close (or flagged unpriced) and converted
 * to the base currency via {@link ExchangeRateProvider}. Positions are read with an {@code @EntityGraph}
 * that fetches their securities in one query, so a portfolio of N holdings costs one positions query plus
 * one latest-price lookup per security - no N+1 over the relationship.
 */
@Service
@Transactional(readOnly = true)
public class PortfolioValuationService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioValuationService.class);
    private static final int PERCENT_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PriceQuoteRepository priceQuoteRepository;
    private final ExchangeRateProvider exchangeRateProvider;

    public PortfolioValuationService(PortfolioRepository portfolioRepository,
                                     PositionRepository positionRepository,
                                     PriceQuoteRepository priceQuoteRepository,
                                     ExchangeRateProvider exchangeRateProvider) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.priceQuoteRepository = priceQuoteRepository;
        this.exchangeRateProvider = exchangeRateProvider;
    }

    public PortfolioValuation valuePortfolio(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> ResourceNotFoundException.of("Portfolio", portfolioId));
        String base = portfolio.getBaseCurrency();
        List<Position> positions = positionRepository.findByPortfolioId(portfolioId);

        List<PositionValuation> rows = new ArrayList<>(positions.size());
        Money totalMarketValue = Money.zero(base);
        Money totalCostBasis = Money.zero(base);
        Money totalUnrealised = Money.zero(base);
        int pricedCount = 0;

        for (Position position : positions) {
            Security security = position.getSecurity();
            Money costBasisBase = exchangeRateProvider.convert(position.getCostBasis(), base);
            Optional<PriceQuote> latest =
                    priceQuoteRepository.findFirstBySecurityIdOrderByAsOfDesc(security.getId());

            if (latest.isPresent()) {
                PriceQuote quote = latest.get();
                Money marketValueBase = exchangeRateProvider.convert(position.marketValue(quote.getClose()), base);
                Money unrealisedBase = marketValueBase.subtract(costBasisBase);

                totalMarketValue = totalMarketValue.add(marketValueBase);
                totalCostBasis = totalCostBasis.add(costBasisBase);
                totalUnrealised = totalUnrealised.add(unrealisedBase);
                pricedCount++;

                rows.add(new PositionValuation(position.getId(), security.getSymbol(), security.getName(),
                        security.getType(), position.getQuantity(), security.getCurrency(),
                        quote.getClose().getAmount(), quote.getAsOf(), true,
                        marketValueBase.getAmount(), costBasisBase.getAmount(), unrealisedBase.getAmount(),
                        percentage(unrealisedBase, costBasisBase)));
            } else {
                rows.add(new PositionValuation(position.getId(), security.getSymbol(), security.getName(),
                        security.getType(), position.getQuantity(), security.getCurrency(),
                        null, null, false,
                        null, costBasisBase.getAmount(), null, null));
            }
        }

        int total = positions.size();
        int unpricedCount = total - pricedCount;
        log.info("Valued portfolio id={} base={} positions={} priced={} unpriced={} marketValue={} unrealised={}",
                portfolioId, base, total, pricedCount, unpricedCount, totalMarketValue, totalUnrealised);

        return new PortfolioValuation(portfolio.getId(), portfolio.getName(), base, Instant.now(),
                totalMarketValue.getAmount(), totalCostBasis.getAmount(), totalUnrealised.getAmount(),
                percentage(totalUnrealised, totalCostBasis), total, pricedCount, unpricedCount, rows);
    }

    /** gain / costBasis as a percentage; null when cost basis is zero (avoids divide-by-zero). */
    private static BigDecimal percentage(Money gain, Money costBasis) {
        if (costBasis.getAmount().signum() == 0) {
            return null;
        }
        return gain.getAmount().multiply(HUNDRED).divide(costBasis.getAmount(), PERCENT_SCALE, ROUNDING);
    }
}
