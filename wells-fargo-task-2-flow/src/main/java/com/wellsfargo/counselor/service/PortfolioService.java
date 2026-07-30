package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.CreatePortfolioRequest;
import com.wellsfargo.counselor.dto.PortfolioResponse;
import com.wellsfargo.counselor.entity.Customer;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.repository.CustomerRepository;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for investment portfolios (lifecycle and lookup). */
@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioRepository portfolioRepository;
    private final CustomerRepository customerRepository;

    public PortfolioService(PortfolioRepository portfolioRepository, CustomerRepository customerRepository) {
        this.portfolioRepository = portfolioRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public PortfolioResponse create(Long customerId, CreatePortfolioRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
        Portfolio portfolio = new Portfolio(request.name(), request.baseCurrency(), customer);
        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Created portfolio '{}' (id={}) for customer id={}", saved.getName(), saved.getId(), customerId);
        return PortfolioResponse.from(saved);
    }

    public PortfolioResponse get(Long id) {
        return portfolioRepository.findById(id)
                .map(PortfolioResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Portfolio", id));
    }

    public Page<PortfolioResponse> listByCustomer(Long customerId, Pageable pageable) {
        return portfolioRepository.findByCustomerId(customerId, pageable).map(PortfolioResponse::from);
    }
}
