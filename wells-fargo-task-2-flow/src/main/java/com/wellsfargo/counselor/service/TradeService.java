package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.ExecuteTradeRequest;
import com.wellsfargo.counselor.dto.TradeResponse;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.Position;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.Trade;
import com.wellsfargo.counselor.entity.TradeSide;
import com.wellsfargo.counselor.exception.InsufficientHoldingsException;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import com.wellsfargo.counselor.repository.PositionRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import com.wellsfargo.counselor.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Executes trades and records the immutable {@link Trade} ledger.
 *
 * <p>{@link #execute} is the integrity-critical path: idempotent on {@code referenceNumber}, it takes a
 * {@code PESSIMISTIC_WRITE} lock on the portfolio so concurrent trades against it are serialised, then
 * finds or creates the {@link Position} and applies the buy/sell via the entity's average-cost logic.
 * The position update and the ledger entry are committed together; the unique constraint on
 * {@code reference_number} is the final backstop against a race.
 */
@Service
@Transactional(readOnly = true)
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final SecurityRepository securityRepository;
    private final TradeRepository tradeRepository;

    public TradeService(PortfolioRepository portfolioRepository, PositionRepository positionRepository,
                        SecurityRepository securityRepository, TradeRepository tradeRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.securityRepository = securityRepository;
        this.tradeRepository = tradeRepository;
    }

    @Transactional
    public TradeResponse execute(Long portfolioId, ExecuteTradeRequest request) {
        if (hasReference(request)) {
            Optional<Trade> replay = tradeRepository.findByReferenceNumber(request.referenceNumber());
            if (replay.isPresent()) {
                log.info("Idempotent trade replay for reference={}", request.referenceNumber());
                return TradeResponse.from(replay.get());
            }
        }

        Portfolio portfolio = portfolioRepository.findByIdForUpdate(portfolioId)
                .orElseThrow(() -> ResourceNotFoundException.of("Portfolio", portfolioId));
        Security security = securityRepository.findById(request.securityId())
                .orElseThrow(() -> ResourceNotFoundException.of("Security", request.securityId()));
        if (!security.getCurrency().equals(request.currency())) {
            throw new IllegalArgumentException("Trade currency " + request.currency()
                    + " must match security currency " + security.getCurrency());
        }

        Money price = Money.of(request.price(), request.currency());
        Position position = positionRepository
                .findByPortfolioIdAndSecurityId(portfolioId, security.getId())
                .orElse(null);

        Trade trade;
        try {
            trade = (request.side() == TradeSide.BUY)
                    ? executeBuy(portfolio, security, position, request.quantity(), price, request.referenceNumber())
                    : executeSell(security, position, request.quantity(), price, request.referenceNumber());
            tradeRepository.saveAndFlush(trade); // flush position update + trade; surface reference races now
        } catch (DataIntegrityViolationException ex) {
            if (hasReference(request)) {
                return tradeRepository.findByReferenceNumber(request.referenceNumber())
                        .map(TradeResponse::from)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }

        log.info("Executed {} {} {} @ {} on portfolio id={} (trade id={})",
                request.side(), request.quantity(), security.getSymbol(), price, portfolioId, trade.getId());
        return TradeResponse.from(trade);
    }

    public Page<TradeResponse> blotter(Long portfolioId, Pageable pageable) {
        return tradeRepository.findByPortfolioIdOrderByExecutedAtDesc(portfolioId, pageable).map(TradeResponse::from);
    }

    private Trade executeBuy(Portfolio portfolio, Security security, Position position,
                             BigDecimal quantity, Money price, String referenceNumber) {
        if (position == null) {
            positionRepository.save(new Position(portfolio, security, quantity, price.multiply(quantity)));
        } else {
            position.applyBuy(quantity, price);
            positionRepository.save(position);
        }
        return Trade.buy(portfolio, security, quantity, price, referenceNumber, Instant.now());
    }

    private Trade executeSell(Security security, Position position, BigDecimal quantity, Money price,
                             String referenceNumber) {
        if (position == null) {
            throw new InsufficientHoldingsException("No position in " + security.getSymbol() + " to sell");
        }
        Money realisedPnL = position.applySell(quantity, price);
        positionRepository.save(position);
        return Trade.sell(position.getPortfolio(), security, quantity, price, realisedPnL, referenceNumber, Instant.now());
    }

    private static boolean hasReference(ExecuteTradeRequest request) {
        return request.referenceNumber() != null && !request.referenceNumber().isBlank();
    }
}
