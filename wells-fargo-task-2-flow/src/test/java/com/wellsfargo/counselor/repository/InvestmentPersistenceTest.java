package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.entity.Advisor;
import com.wellsfargo.counselor.entity.Customer;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.Position;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.SecurityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Persistence-slice test for the investment domain: mappings, unique links, price queries, auditing. */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class InvestmentPersistenceTest {

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
    private PositionRepository positionRepository;

    private Security security;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        Advisor advisor = new Advisor("Ada", "Lovelace", "1 Analytical Way", "+1 555 0100", "ada@wf.test");
        Customer customer = new Customer("Grace", "Hopper", "grace@wf.test", "+1 555 0101",
                "2 Compiler Rd", LocalDate.of(1980, 1, 1));
        advisor.addCustomer(customer);
        advisorRepository.save(advisor);
        customerRepository.save(customer);

        security = securityRepository.save(
                new Security("ACME", "Acme Corp", SecurityType.COMMON_STOCK, "USD", "NYSE", null));

        portfolio = new Portfolio("Growth", "USD", customer);
        Position position = new Position(portfolio, security, new BigDecimal("10"), Money.of("1000.00", "USD"));
        portfolio.addPosition(position);
        portfolioRepository.save(portfolio); // cascades the position
    }

    @Test
    void persistsPortfolioWithPositionAndAuditing() {
        Optional<Position> found =
                positionRepository.findByPortfolioIdAndSecurityId(portfolio.getId(), security.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualByComparingTo("10");
        assertThat(found.get().getCostBasis().getAmount()).isEqualByComparingTo("1000.00");
        assertThat(found.get().getCostBasis().getCurrency()).isEqualTo("USD");
        assertThat(found.get().getCreatedAt()).isNotNull(); // JPA auditing active
    }

    @Test
    void latestAndRangePriceQueriesWork() {
        priceQuoteRepository.save(PriceQuote.of(security, LocalDate.of(2026, 1, 2), Money.of("100.00", "USD"), 1000L));
        priceQuoteRepository.save(PriceQuote.of(security, LocalDate.of(2026, 1, 5), Money.of("110.00", "USD"), 1200L));
        priceQuoteRepository.save(PriceQuote.of(security, LocalDate.of(2026, 1, 9), Money.of("105.00", "USD"), 900L));

        Optional<PriceQuote> latest = priceQuoteRepository.findFirstBySecurityIdOrderByAsOfDesc(security.getId());
        assertThat(latest).isPresent();
        assertThat(latest.get().getAsOf()).isEqualTo(LocalDate.of(2026, 1, 9));
        assertThat(latest.get().getClose().getAmount()).isEqualByComparingTo("105.00");

        List<PriceQuote> history = priceQuoteRepository.findBySecurityIdAndAsOfBetweenOrderByAsOfAsc(
                security.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 6));
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getAsOf()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(history.get(1).getAsOf()).isEqualTo(LocalDate.of(2026, 1, 5));
    }
}
