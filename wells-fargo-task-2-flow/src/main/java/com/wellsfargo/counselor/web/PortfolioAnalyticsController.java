package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.PortfolioAnalytics;
import com.wellsfargo.counselor.service.PortfolioAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioAnalyticsController {

    private final PortfolioAnalyticsService analyticsService;

    public PortfolioAnalyticsController(PortfolioAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{portfolioId}/analytics")
    public PortfolioAnalytics analytics(
            @PathVariable Long portfolioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long benchmarkId,
            @RequestParam(required = false) BigDecimal riskFreeRate,
            @RequestParam(required = false) Integer periodsPerYear) {
        return analyticsService.analyze(portfolioId, from, to, benchmarkId, riskFreeRate, periodsPerYear);
    }
}
