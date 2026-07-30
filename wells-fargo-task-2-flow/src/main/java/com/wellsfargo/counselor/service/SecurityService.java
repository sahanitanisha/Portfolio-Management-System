package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.CreateSecurityRequest;
import com.wellsfargo.counselor.dto.IngestPriceRequest;
import com.wellsfargo.counselor.dto.PriceQuoteResponse;
import com.wellsfargo.counselor.dto.SecurityResponse;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.event.PriceIngestedEvent;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference-data and market-data ingestion service. Price ingestion is idempotent on
 * {@code (security, date)}: re-ingesting an existing bar returns the stored quote rather than creating
 * a duplicate, which keeps the immutable price series clean under retried feeds.
 */
@Service
@Transactional(readOnly = true)
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    private final SecurityRepository securityRepository;
    private final PriceQuoteRepository priceQuoteRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SecurityService(SecurityRepository securityRepository, PriceQuoteRepository priceQuoteRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.securityRepository = securityRepository;
        this.priceQuoteRepository = priceQuoteRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SecurityResponse create(CreateSecurityRequest request) {
        if (securityRepository.existsBySymbol(request.symbol())) {
            throw new IllegalArgumentException("Security symbol already exists: " + request.symbol());
        }
        Security security = new Security(request.symbol(), request.name(), request.type(),
                request.currency(), request.exchange(), request.isin());
        Security saved = securityRepository.save(security);
        log.info("Created security {} (id={})", saved.getSymbol(), saved.getId());
        return SecurityResponse.from(saved);
    }

    public SecurityResponse get(Long id) {
        return securityRepository.findById(id)
                .map(SecurityResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Security", id));
    }

    public Page<SecurityResponse> list(Pageable pageable) {
        return securityRepository.findAll(pageable).map(SecurityResponse::from);
    }

    @Transactional
    public PriceQuoteResponse ingestPrice(Long securityId, IngestPriceRequest request) {
        Security security = securityRepository.findById(securityId)
                .orElseThrow(() -> ResourceNotFoundException.of("Security", securityId));
        PriceQuoteResponse response = PriceQuoteResponse.from(saveQuoteIfAbsent(security, request));
        eventPublisher.publishEvent(new PriceIngestedEvent(securityId));
        return response;
    }

    @Transactional
    public List<PriceQuoteResponse> ingestPrices(Long securityId, List<IngestPriceRequest> requests) {
        Security security = securityRepository.findById(securityId)
                .orElseThrow(() -> ResourceNotFoundException.of("Security", securityId));
        List<PriceQuoteResponse> results = new ArrayList<>(requests.size());
        for (IngestPriceRequest request : requests) {
            results.add(PriceQuoteResponse.from(saveQuoteIfAbsent(security, request)));
        }
        log.info("Ingested {} price point(s) for security id={}", requests.size(), securityId);
        eventPublisher.publishEvent(new PriceIngestedEvent(securityId));
        return results;
    }

    private PriceQuote saveQuoteIfAbsent(Security security, IngestPriceRequest request) {
        return priceQuoteRepository.findBySecurityIdAndAsOf(security.getId(), request.asOf())
                .orElseGet(() -> priceQuoteRepository.save(PriceQuote.of(
                        security, request.asOf(), Money.of(request.close(), security.getCurrency()), request.volume())));
    }
}
