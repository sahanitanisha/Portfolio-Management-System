package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.AdvisorResponse;
import com.wellsfargo.counselor.dto.CreateAdvisorRequest;
import com.wellsfargo.counselor.entity.Advisor;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.repository.AdvisorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for advisors. Read methods inherit the class-level read-only transaction;
 * mutating methods declare their own read-write transaction.
 */
@Service
@Transactional(readOnly = true)
public class AdvisorService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);

    private final AdvisorRepository advisorRepository;

    public AdvisorService(AdvisorRepository advisorRepository) {
        this.advisorRepository = advisorRepository;
    }

    @Transactional
    public AdvisorResponse create(CreateAdvisorRequest request) {
        if (advisorRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }
        Advisor advisor = new Advisor(request.firstName(), request.lastName(),
                request.address(), request.phone(), request.email());
        Advisor saved = advisorRepository.save(advisor);
        log.info("Created advisor id={}", saved.getId());
        return AdvisorResponse.from(saved);
    }

    public AdvisorResponse get(Long id) {
        return advisorRepository.findById(id)
                .map(AdvisorResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Advisor", id));
    }

    public Page<AdvisorResponse> list(Pageable pageable) {
        return advisorRepository.findAll(pageable).map(AdvisorResponse::from);
    }
}
