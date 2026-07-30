package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.dto.PortfolioValuation;
import com.wellsfargo.counselor.entity.Advisor;
import com.wellsfargo.counselor.entity.Customer;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.Position;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.SecurityType;
import com.wellsfargo.counselor.exception.ExchangeRateUnavailableException;
import com.wellsfargo.counselor.fx.InMemoryExchangeRateProvider;
import com.wellsfargo.counselor.repository.AdvisorRepository;
import com.wellsfargo.counselor.repository.CustomerRepository;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Slice test for {@link PortfolioValuationService}: single-currency, unpriced, cross-currency, fail-fast. */
@DataJpaTest
@Import({PortfolioValuationService.class, InMemoryExchangeRateProvider.class, JpaAuditingConfig.class})
class PortfolioValuationServiceTest {

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
    private PortfolioValuationService valuationService;
    @Autowired
    private InMemoryExchangeRateProvider exchangeRateProvider;

    private Customer customer;

    @BeforeEach
    void setUp() {
        Advisor advisor = new Advisor("Ada", "Lovelace", "1 Analytical Way", "+1 555 0100", "ada@wf.test");
        customer = new Customer("Grace", "Hopper", "grace@wf.test", "+1 555 0101", "2 Compiler Rd",
                LocalDate.of(1980, 1, 1));
        advisor.addCustomer(customer);
        advisorRepository.save(advisor);
        customerRepository.save(customer);
    }

    private Security saveSecurity(String symbol, String currency) {
        return securityRepository.save(
                new Security(symbol, symbol + " Inc", SecurityType.COMMON_STOCK, currency, "NYSE", null));
    }

    private Portfolio savePortfolio(String baseCurrency) {
        return portfolioRepository.save(new Portfolio("Growth", baseCurrency, customer));
    }

    private void addPosition(Portfolio portfolio, Security security, String quantity, String totalCost) {
        Position position = new Position(portfolio, security, new BigDecimal(quantity),
                Money.of(totalCost, security.getCurrency()));
        portfolio.addPosition(position);
        portfolioRepository.save(portfolio);
    }

    private void savePrice(Security security, String date, String close) {
        priceQuoteRepository.save(
                PriceQuote.of(security, LocalDate.parse(date), Money.of(close, security.getCurrency()), 1000L));
    }

    @Test
    void valuesSingleCurrencyPortfolio() {
        Portfolio portfolio = savePortfolio("USD");
        Security acme = saveSecurity("ACME", "USD");
        addPosition(portfolio, acme, "10", "1000.00"); // avg cost 100
        savePrice(acme, "2026-06-15", "130.00");

        PortfolioValuation valuation = valuationService.valuePortfolio(portfolio.getId());

        assertThat(valuation.baseCurrency()).isEqualTo("USD");
        assertThat(valuation.totalMarketValue()).isEqualByComparingTo("1300.00");
        assertThat(valuation.totalCostBasis()).isEqualByComparingTo("1000.00");
        assertThat(valuation.totalUnrealisedGain()).isEqualByComparingTo("300.00");
        assertThat(valuation.totalUnrealisedGainPct()).isEqualByComparingTo("30.0000");
        assertThat(valuation.pricedCount()).isEqualTo(1);
        assertThat(valuation.unpricedCount()).isZero();
    }

    @Test
    void flagsUnpricedPositionsAndExcludesThemFromTotals() {
        Portfolio portfolio = savePortfolio("USD");
        Security acme = saveSecurity("ACME", "USD");
        Security unpriced = saveSecurity("NOPX", "USD");
        addPosition(portfolio, acme, "10", "1000.00");
        addPosition(portfolio, unpriced, "5", "500.00");
        savePrice(acme, "2026-06-15", "130.00");

        PortfolioValuation valuation = valuationService.valuePortfolio(portfolio.getId());

        assertThat(valuation.positionCount()).isEqualTo(2);
        assertThat(valuation.pricedCount()).isEqualTo(1);
        assertThat(valuation.unpricedCount()).isEqualTo(1);
        assertThat(valuation.totalMarketValue()).isEqualByComparingTo("1300.00"); // unpriced excluded
        assertThat(valuation.totalCostBasis()).isEqualByComparingTo("1000.00");
    }

    @Test
    void convertsCrossCurrencyHoldingsToBase() {
        exchangeRateProvider.upsertRate("EUR", "USD", new BigDecimal("1.10"));
        Portfolio portfolio = savePortfolio("USD");
        Security euroSec = saveSecurity("EURO", "EUR");
        addPosition(portfolio, euroSec, "10", "1000.00"); // 1000 EUR cost
        savePrice(euroSec, "2026-06-15", "130.00");        // 130 EUR close

        PortfolioValuation valuation = valuationService.valuePortfolio(portfolio.getId());

        // market value 1300 EUR * 1.10 = 1430 USD; cost 1000 EUR * 1.10 = 1100 USD; unrealised 330 USD
        assertThat(valuation.totalMarketValue()).isEqualByComparingTo("1430.00");
        assertThat(valuation.totalCostBasis()).isEqualByComparingTo("1100.00");
        assertThat(valuation.totalUnrealisedGain()).isEqualByComparingTo("330.00");
    }

    @Test
    void failsFastWhenExchangeRateMissing() {
        Portfolio portfolio = savePortfolio("USD");
        Security gbpSec = saveSecurity("LON", "GBP");
        addPosition(portfolio, gbpSec, "10", "1000.00");
        savePrice(gbpSec, "2026-06-15", "130.00");

        assertThatThrownBy(() -> valuationService.valuePortfolio(portfolio.getId()))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
