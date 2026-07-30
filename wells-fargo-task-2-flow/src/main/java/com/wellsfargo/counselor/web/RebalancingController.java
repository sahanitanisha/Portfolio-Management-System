package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.RebalancePlan;
import com.wellsfargo.counselor.dto.RebalanceResult;
import com.wellsfargo.counselor.dto.SetTargetAllocationsRequest;
import com.wellsfargo.counselor.dto.TargetAllocationResponse;
import com.wellsfargo.counselor.service.RebalancingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}")
public class RebalancingController {

    private final RebalancingService rebalancingService;

    public RebalancingController(RebalancingService rebalancingService) {
        this.rebalancingService = rebalancingService;
    }

    @PutMapping("/target-allocations")
    public List<TargetAllocationResponse> setTargets(@PathVariable Long portfolioId,
                                                     @Valid @RequestBody SetTargetAllocationsRequest request) {
        return rebalancingService.setTargets(portfolioId, request);
    }

    @GetMapping("/target-allocations")
    public List<TargetAllocationResponse> getTargets(@PathVariable Long portfolioId) {
        return rebalancingService.getTargets(portfolioId);
    }

    @GetMapping("/rebalance-plan")
    public RebalancePlan plan(@PathVariable Long portfolioId,
                              @RequestParam(defaultValue = "0") BigDecimal driftThreshold) {
        return rebalancingService.computePlan(portfolioId, driftThreshold);
    }

    @PostMapping("/rebalance")
    public RebalanceResult rebalance(@PathVariable Long portfolioId,
                                     @RequestParam(defaultValue = "0") BigDecimal driftThreshold) {
        return rebalancingService.execute(portfolioId, driftThreshold);
    }
}
