package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.RebalancePlan;
import com.wellsfargo.counselor.dto.RebalanceProposedTrade;
import com.wellsfargo.counselor.dto.RebalanceResult;
import com.wellsfargo.counselor.dto.SetTargetAllocationsRequest;
import com.wellsfargo.counselor.dto.TargetAllocationEntry;
import com.wellsfargo.counselor.dto.TargetAllocationResponse;
import com.wellsfargo.counselor.dto.ExecuteTradeRequest;
import com.wellsfargo.counselor.dto.TradeResponse;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.Position;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.TargetAllocation;
import com.wellsfargo.counselor.entity.TradeSide;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.fx.ExchangeRateProvider;
import com.wellsfargo.counselor.rebalance.RebalanceInput;
import com.wellsfargo.counselor.rebalance.RebalancingCalculator;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import com.wellsfargo.counselor.repository.PositionRepository;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import com.wellsfargo.counselor.repository.TargetAllocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Automated rebalancing: maintains a portfolio's target allocations and computes/executes the trades
 * required to move current holdings toward those targets.
 *
 * <p>Targets redistribute the portfolio's current invested value (cash funding is out of scope here),
 * so the sum of buys and sells nets out. Execution places sells before buys and routes every order
 * through {@link TradeService}, inheriting its pessimistic lock, idempotency and average-cost handling.
 */
@Service
@Transactional(readOnly = true)
public class RebalancingService {

    private static final Logger log = LoggerFactory.getLogger(RebalancingService.class);

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final SecurityRepository securityRepository;
    private final PriceQuoteRepository priceQuoteRepository;
    private final TargetAllocationRepository targetAllocationRepository;
    private final ExchangeRateProvider exchangeRateProvider;
    private final TradeService tradeService;

    public RebalancingService(PortfolioRepository portfolioRepository, PositionRepository positionRepository,
                              SecurityRepository securityRepository, PriceQuoteRepository priceQuoteRepository,
                              TargetAllocationRepository targetAllocationRepository,
                              ExchangeRateProvider exchangeRateProvider, TradeService tradeService) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.securityRepository = securityRepository;
        this.priceQuoteRepository = priceQuoteRepository;
        this.targetAllocationRepository = targetAllocationRepository;
        this.exchangeRateProvider = exchangeRateProvider;
        this.tradeService = tradeService;
    }

    @Transactional
    public List<TargetAllocationResponse> setTargets(Long portfolioId, SetTargetAllocationsRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> ResourceNotFoundException.of("Portfolio", portfolioId));
        BigDecimal sum = request.allocations().stream()
                .map(TargetAllocationEntry::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Target weights sum to " + sum + ", which exceeds 1.0");
        }
        targetAllocationRepository.deleteByPortfolioId(portfolioId);
        List<TargetAllocationResponse> result = new ArrayList<>();
        for (TargetAllocationEntry entry : request.allocations()) {
            Security security = securityRepository.findById(entry.securityId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Security", entry.securityId()));
            TargetAllocation saved = targetAllocationRepository.save(
                    new TargetAllocation(portfolio, security, entry.weight()));
            result.add(TargetAllocationResponse.from(saved));
        }
        log.info("Set {} target allocation(s) for portfolio id={}", result.size(), portfolioId);
        return result;
    }

    public List<TargetAllocationResponse> getTargets(Long portfolioId) {
        return targetAllocationRepository.findByPortfolioId(portfolioId).stream()
                .map(TargetAllocationResponse::from)
                .toList();
    }

    public RebalancePlan computePlan(Long portfolioId, BigDecimal driftThreshold) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> ResourceNotFoundException.of("Portfolio", portfolioId));
        String base = portfolio.getBaseCurrency();

        Map<Long, Position> positionsBySecurity = new LinkedHashMap<>();
        for (Position position : positionRepository.findByPortfolioId(portfolioId)) {
            positionsBySecurity.put(position.getSecurity().getId(), position);
        }
        Map<Long, BigDecimal> targetWeights = new LinkedHashMap<>();
        for (TargetAllocation allocation : targetAllocationRepository.findByPortfolioId(portfolioId)) {
            targetWeights.put(allocation.getSecurity().getId(), allocation.getTargetWeight());
        }

        Set<Long> securityIds = new LinkedHashSet<>();
        securityIds.addAll(targetWeights.keySet());
        securityIds.addAll(positionsBySecurity.keySet());

        List<RebalanceInput> lines = new ArrayList<>();
        Money totalValue = Money.zero(base);
        for (Long securityId : securityIds) {
            Position position = positionsBySecurity.get(securityId);
            Security security = (position != null)
                    ? position.getSecurity()
                    : securityRepository.findById(securityId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Security", securityId));

            Optional<PriceQuote> latest = priceQuoteRepository.findFirstBySecurityIdOrderByAsOfDesc(securityId);
            BigDecimal priceBase = latest
                    .map(quote -> exchangeRateProvider.convert(quote.getClose(), base).getAmount())
                    .orElse(null);

            BigDecimal heldQuantity = (position != null) ? position.getQuantity() : BigDecimal.ZERO;
            BigDecimal currentValueBase = BigDecimal.ZERO;
            if (position != null && latest.isPresent()) {
                Money marketValueBase = exchangeRateProvider.convert(position.marketValue(latest.get().getClose()), base);
                currentValueBase = marketValueBase.getAmount();
                totalValue = totalValue.add(marketValueBase);
            }
            lines.add(new RebalanceInput(securityId, security.getSymbol(), currentValueBase, priceBase,
                    heldQuantity, targetWeights.getOrDefault(securityId, BigDecimal.ZERO)));
        }

        BigDecimal threshold = (driftThreshold != null) ? driftThreshold : BigDecimal.ZERO;
        List<RebalanceProposedTrade> proposed = RebalancingCalculator.plan(lines, totalValue.getAmount(), threshold);
        return new RebalancePlan(portfolioId, base, totalValue.getAmount(), threshold, proposed);
    }

    @Transactional
    public RebalanceResult execute(Long portfolioId, BigDecimal driftThreshold) {
        RebalancePlan plan = computePlan(portfolioId, driftThreshold);
        String runId = UUID.randomUUID().toString();

        List<RebalanceProposedTrade> ordered = new ArrayList<>(plan.proposedTrades());
        ordered.sort(Comparator.comparingInt(trade -> trade.side() == TradeSide.SELL ? 0 : 1)); // sells fund buys

        List<TradeResponse> executed = new ArrayList<>();
        for (RebalanceProposedTrade trade : ordered) {
            PriceQuote latest = priceQuoteRepository.findFirstBySecurityIdOrderByAsOfDesc(trade.securityId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Price for security", trade.securityId()));
            Money price = latest.getClose();
            ExecuteTradeRequest request = new ExecuteTradeRequest(trade.securityId(), trade.side(), trade.quantity(),
                    price.getAmount(), price.getCurrency(), "rebalance:" + runId + ":" + trade.securityId());
            executed.add(tradeService.execute(portfolioId, request));
        }
        log.info("Rebalanced portfolio id={} with {} trade(s) (run {})", portfolioId, executed.size(), runId);
        return new RebalanceResult(portfolioId, executed.size(), executed);
    }
}
