package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.dto.PortfolioAnalytics;
import com.wellsfargo.counselor.entity.Advisor;
import com.wellsfargo.counselor.entity.Customer;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.SecurityType;
import com.wellsfargo.counselor.entity.Trade;
import com.wellsfargo.counselor.exception.InsufficientDataException;
import com.wellsfargo.counselor.repository.AdvisorRepository;
import com.wellsfargo.counselor.repository.CustomerRepository;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import com.wellsfargo.counselor.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Slice test for {@link PortfolioAnalyticsService}: trade-aware (as-of holdings) value series. */
@DataJpaTest
@Import({PortfolioAnalyticsService.class, HistoricalExchangeRateService.class, JpaAuditingConfig.class})
class PortfolioAnalyticsServiceTest {

    private static final LocalDate FROM = LocalDate.parse("2026-01-01");
    private static final LocalDate TO = LocalDate.parse("2026-12-31");

    @Autowired
    private AdvisorRepository advisorRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private SecurityRepository securityRepository;
    @Autowired
    private PriceQuoteRepository priceQuoteRepository;
    @Autowired
    private PortfolioRepository portfolioRepository;
    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private PortfolioAnalyticsService analyticsService;

    private Portfolio newPortfolio() {
        Advisor advisor = new Advisor("Ada", "Lovelace", "1 Analytical Way", "+1 555 0100", "ada@wf.test");
        Customer customer = new Customer("Grace", "Hopper", "grace@wf.test", "+1 555 0101", "2 Compiler Rd",
                LocalDate.of(1980, 1, 1));
        advisor.addCustomer(customer);
        advisorRepository.save(advisor);
        customerRepository.save(customer);
        return portfolioRepository.save(new Portfolio("Growth", "USD", customer));
    }

    private Security newSecurity(String symbol) {
        return securityRepository.save(new Security(symbol, symbol + " Inc", SecurityType.COMMON_STOCK, "USD", "NYSE", null));
    }

    private void savePrice(Security security, String date, String close) {
        priceQuoteRepository.save(PriceQuote.of(security, LocalDate.parse(date), Money.of(close, "USD"), null));
    }

    private void backdatedBuy(Portfolio portfolio, Security security, String quantity, String price,
                              String executedAtIso, String reference) {
        tradeRepository.save(Trade.buy(portfolio, security, new BigDecimal(quantity),
                Money.of(price, "USD"), reference, Instant.parse(executedAtIso)));
    }

    @Test
    void computesAnalyticsFromReconstructedHoldings() {
        Portfolio portfolio = newPortfolio();
        Security acme = newSecurity("ACME");
        backdatedBuy(portfolio, acme, "10", "100.00", "2026-01-01T00:00:00Z", "h1"); // held before all prices
        savePrice(acme, "2026-01-02", "100.00"); // value 1000
        savePrice(acme, "2026-01-03", "120.00"); // value 1200
        savePrice(acme, "2026-01-04", "108.00"); // value 1080

        PortfolioAnalytics analytics = analyticsService.analyze(portfolio.getId(), FROM, TO, null, null, null);

        assertThat(analytics.observations()).isEqualTo(2);
        assertThat(analytics.maxDrawdown()).isEqualByComparingTo("0.1"); // (1200 - 1080) / 1200
        assertThat(analytics.annualizedVolatility()).isNotNull();
        assertThat(analytics.sharpeRatio()).isNotNull();
    }

    @Test
    void rejectsPortfolioWithoutTrades() {
        Portfolio portfolio = newPortfolio();
        Security acme = newSecurity("ACME");
        savePrice(acme, "2026-01-02", "100.00");
        savePrice(acme, "2026-01-03", "120.00");
        savePrice(acme, "2026-01-04", "108.00");

        assertThatThrownBy(() -> analyticsService.analyze(portfolio.getId(), FROM, TO, null, null, null))
                .isInstanceOf(InsufficientDataException.class);
    }

    @Test
    void rejectsInsufficientPriceHistory() {
        Portfolio portfolio = newPortfolio();
        Security acme = newSecurity("ACME");
        backdatedBuy(portfolio, acme, "10", "100.00", "2026-01-01T00:00:00Z", "h1");
        savePrice(acme, "2026-01-02", "100.00");
        savePrice(acme, "2026-01-03", "120.00"); // only 2 valuation dates

        assertThatThrownBy(() -> analyticsService.analyze(portfolio.getId(), FROM, TO, null, null, null))
                .isInstanceOf(InsufficientDataException.class);
    }
}
