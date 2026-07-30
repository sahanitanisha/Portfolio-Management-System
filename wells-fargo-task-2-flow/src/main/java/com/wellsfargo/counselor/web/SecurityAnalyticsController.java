package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.SecurityAnalytics;
import com.wellsfargo.counselor.service.SecurityAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/securities")
public class SecurityAnalyticsController {

    private final SecurityAnalyticsService analyticsService;

    public SecurityAnalyticsController(SecurityAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Risk/return analytics for a security over [from, to]. Optional {@code benchmarkId} adds beta/alpha;
     * {@code riskFreeRate} is an annual rate (default 0); {@code periodsPerYear} sets the annualisation
     * factor (default 252 for daily data).
     */
    @GetMapping("/{securityId}/analytics")
    public SecurityAnalytics analytics(
            @PathVariable Long securityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long benchmarkId,
            @RequestParam(required = false) BigDecimal riskFreeRate,
            @RequestParam(required = false) Integer periodsPerYear) {
        return analyticsService.analyze(securityId, from, to, benchmarkId, riskFreeRate, periodsPerYear);
    }
}
