package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.dto.SecurityAnalytics;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.SecurityType;
import com.wellsfargo.counselor.exception.InsufficientDataException;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Slice test for {@link SecurityAnalyticsService}: history-driven metrics, benchmark beta, fail-fast. */
@DataJpaTest
@Import({SecurityAnalyticsService.class, JpaAuditingConfig.class})
class SecurityAnalyticsServiceTest {

    private static final LocalDate FROM = LocalDate.parse("2026-01-01");
    private static final LocalDate TO = LocalDate.parse("2026-12-31");

    @Autowired
    private SecurityRepository securityRepository;
    @Autowired
    private PriceQuoteRepository priceQuoteRepository;
    @Autowired
    private SecurityAnalyticsService analyticsService;

    private Security saveSecurity(String symbol) {
        return securityRepository.save(
                new Security(symbol, symbol + " Inc", SecurityType.COMMON_STOCK, "USD", "NYSE", null));
    }

    private void savePrice(Security security, String date, String close) {
        priceQuoteRepository.save(
                PriceQuote.of(security, LocalDate.parse(date), Money.of(close, "USD"), 1000L));
    }

    @Test
    void computesVolatilitySharpeAndDrawdown() {
        Security acme = saveSecurity("ACME");
        savePrice(acme, "2026-01-02", "100.00");
        savePrice(acme, "2026-01-03", "120.00");
        savePrice(acme, "2026-01-04", "108.00");

        SecurityAnalytics analytics = analyticsService.analyze(acme.getId(), FROM, TO, null, null, null);

        assertThat(analytics.symbol()).isEqualTo("ACME");
        assertThat(analytics.observations()).isEqualTo(2);
        assertThat(analytics.periodsPerYear()).isEqualTo(252);
        assertThat(analytics.annualizedVolatility()).isNotNull();
        assertThat(analytics.sharpeRatio()).isNotNull();
        assertThat(analytics.maxDrawdown()).isNotNull();
        assertThat(analytics.beta()).isNull();
        assertThat(analytics.benchmarkSymbol()).isNull();
    }

    @Test
    void computesBetaAndAlphaAgainstBenchmark() {
        Security acme = saveSecurity("ACME");
        Security index = saveSecurity("IDX");
        savePrice(acme, "2026-01-02", "100.00");
        savePrice(acme, "2026-01-03", "120.00");
        savePrice(acme, "2026-01-04", "108.00");
        savePrice(index, "2026-01-02", "50.00");
        savePrice(index, "2026-01-03", "55.00");
        savePrice(index, "2026-01-04", "53.00");

        SecurityAnalytics analytics = analyticsService.analyze(acme.getId(), FROM, TO, index.getId(), null, null);

        assertThat(analytics.benchmarkSymbol()).isEqualTo("IDX");
        assertThat(analytics.beta()).isNotNull();
        assertThat(analytics.alpha()).isNotNull();
    }

    @Test
    void rejectsInsufficientHistory() {
        Security acme = saveSecurity("ACME");
        savePrice(acme, "2026-01-02", "100.00");
        savePrice(acme, "2026-01-03", "120.00"); // only 2 points (< 3)

        assertThatThrownBy(() -> analyticsService.analyze(acme.getId(), FROM, TO, null, null, null))
                .isInstanceOf(InsufficientDataException.class);
    }
}
