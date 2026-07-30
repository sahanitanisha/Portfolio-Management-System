package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.AdvisorResponse;
import com.wellsfargo.counselor.dto.CreateAdvisorRequest;
import com.wellsfargo.counselor.dto.CustomerResponse;
import com.wellsfargo.counselor.service.AdvisorService;
import com.wellsfargo.counselor.service.CustomerService;
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
@RequestMapping("/api/v1/advisors")
public class AdvisorController {

    private final AdvisorService advisorService;
    private final CustomerService customerService;

    public AdvisorController(AdvisorService advisorService, CustomerService customerService) {
        this.advisorService = advisorService;
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<AdvisorResponse> create(@Valid @RequestBody CreateAdvisorRequest request) {
        AdvisorResponse created = advisorService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/advisors/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public AdvisorResponse get(@PathVariable Long id) {
        return advisorService.get(id);
    }

    @GetMapping
    public Page<AdvisorResponse> list(Pageable pageable) {
        return advisorService.list(pageable);
    }

    @GetMapping("/{id}/customers")
    public Page<CustomerResponse> customers(@PathVariable Long id, Pageable pageable) {
        return customerService.listByAdvisor(id, pageable);
    }
}
