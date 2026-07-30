package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.CreateSecurityRequest;
import com.wellsfargo.counselor.dto.IngestPriceRequest;
import com.wellsfargo.counselor.dto.IngestPricesRequest;
import com.wellsfargo.counselor.dto.PriceQuoteResponse;
import com.wellsfargo.counselor.dto.SecurityResponse;
import com.wellsfargo.counselor.service.SecurityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/securities")
public class SecurityController {

    private final SecurityService securityService;

    public SecurityController(SecurityService securityService) {
        this.securityService = securityService;
    }

    @PostMapping
    public ResponseEntity<SecurityResponse> create(@Valid @RequestBody CreateSecurityRequest request) {
        SecurityResponse created = securityService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/securities/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public SecurityResponse get(@PathVariable Long id) {
        return securityService.get(id);
    }

    @GetMapping
    public Page<SecurityResponse> list(Pageable pageable) {
        return securityService.list(pageable);
    }

    @PostMapping("/{securityId}/prices")
    public ResponseEntity<PriceQuoteResponse> ingestPrice(@PathVariable Long securityId,
                                                          @Valid @RequestBody IngestPriceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.ingestPrice(securityId, request));
    }

    @PostMapping("/{securityId}/prices/batch")
    public List<PriceQuoteResponse> ingestPrices(@PathVariable Long securityId,
                                                 @Valid @RequestBody IngestPricesRequest request) {
        return securityService.ingestPrices(securityId, request.prices());
    }
}
