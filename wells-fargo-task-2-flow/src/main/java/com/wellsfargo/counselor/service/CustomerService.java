package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.CreateCustomerRequest;
import com.wellsfargo.counselor.dto.CustomerResponse;
import com.wellsfargo.counselor.entity.Advisor;
import com.wellsfargo.counselor.entity.Customer;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.repository.AdvisorRepository;
import com.wellsfargo.counselor.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for clients. Enforces the advisor relationship and email uniqueness. */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final AdvisorRepository advisorRepository;

    public CustomerService(CustomerRepository customerRepository, AdvisorRepository advisorRepository) {
        this.customerRepository = customerRepository;
        this.advisorRepository = advisorRepository;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }
        Advisor advisor = advisorRepository.findById(request.advisorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Advisor", request.advisorId()));
        Customer customer = new Customer(request.firstName(), request.lastName(), request.email(),
                request.phoneNumber(), request.address(), request.dateOfBirth());
        advisor.addCustomer(customer); // keeps both sides of the association consistent
        Customer saved = customerRepository.save(customer);
        log.info("Created customer id={} under advisor id={}", saved.getId(), advisor.getId());
        return CustomerResponse.from(saved);
    }

    public CustomerResponse get(Long id) {
        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }

    public Page<CustomerResponse> listByAdvisor(Long advisorId, Pageable pageable) {
        return customerRepository.findByAdvisorId(advisorId, pageable).map(CustomerResponse::from);
    }
}
