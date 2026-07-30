package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.CreatePortfolioRequest;
import com.wellsfargo.counselor.dto.PortfolioResponse;
import com.wellsfargo.counselor.dto.PortfolioValuation;
import com.wellsfargo.counselor.service.PortfolioService;
import com.wellsfargo.counselor.service.PortfolioValuationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioValuationService valuationService;

    public PortfolioController(PortfolioService portfolioService, PortfolioValuationService valuationService) {
        this.portfolioService = portfolioService;
        this.valuationService = valuationService;
    }

    @PostMapping("/customers/{customerId}/portfolios")
    public ResponseEntity<PortfolioResponse> create(@PathVariable Long customerId,
                                                    @Valid @RequestBody CreatePortfolioRequest request) {
        PortfolioResponse created = portfolioService.create(customerId, request);
        return ResponseEntity.created(URI.create("/api/v1/portfolios/" + created.id())).body(created);
    }

    @GetMapping("/portfolios/{id}")
    public PortfolioResponse get(@PathVariable Long id) {
        return portfolioService.get(id);
    }

    @GetMapping("/customers/{customerId}/portfolios")
    public Page<PortfolioResponse> listByCustomer(@PathVariable Long customerId, Pageable pageable) {
        return portfolioService.listByCustomer(customerId, pageable);
    }

    @GetMapping("/portfolios/{portfolioId}/valuation")
    public PortfolioValuation valuation(@PathVariable Long portfolioId) {
        return valuationService.valuePortfolio(portfolioId);
    }
}
