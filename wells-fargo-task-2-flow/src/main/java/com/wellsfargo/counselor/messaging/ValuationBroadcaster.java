package com.wellsfargo.counselor.messaging;

import com.wellsfargo.counselor.dto.PortfolioValuation;
import com.wellsfargo.counselor.event.PriceIngestedEvent;
import com.wellsfargo.counselor.repository.PositionRepository;
import com.wellsfargo.counselor.service.PortfolioValuationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Pushes refreshed valuations to subscribers when a price is ingested. Runs after the ingesting
 * transaction commits (so the new price is visible), recomputes each affected portfolio, and sends to
 * its STOMP topic. A failure for one portfolio (e.g. a missing FX rate) is logged and skipped so it
 * cannot block updates for the others - graceful degradation.
 */
@Component
public class ValuationBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ValuationBroadcaster.class);

    private final PositionRepository positionRepository;
    private final PortfolioValuationService valuationService;
    private final SimpMessagingTemplate messagingTemplate;

    public ValuationBroadcaster(PositionRepository positionRepository,
                                PortfolioValuationService valuationService,
                                SimpMessagingTemplate messagingTemplate) {
        this.positionRepository = positionRepository;
        this.valuationService = valuationService;
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener
    public void onPriceIngested(PriceIngestedEvent event) {
        List<Long> portfolioIds = positionRepository.findPortfolioIdsHoldingSecurity(event.securityId());
        for (Long portfolioId : portfolioIds) {
            try {
                PortfolioValuation valuation = valuationService.valuePortfolio(portfolioId);
                messagingTemplate.convertAndSend("/topic/portfolios/" + portfolioId + "/valuation", valuation);
            } catch (RuntimeException ex) {
                log.warn("Skipped valuation broadcast for portfolio id={}: {}", portfolioId, ex.getMessage());
            }
        }
    }
}
