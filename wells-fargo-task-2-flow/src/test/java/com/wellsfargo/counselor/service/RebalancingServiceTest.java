package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.dto.ExecuteTradeRequest;
import com.wellsfargo.counselor.dto.RebalancePlan;
import com.wellsfargo.counselor.dto.RebalanceProposedTrade;
import com.wellsfargo.counselor.dto.RebalanceResult;
import com.wellsfargo.counselor.dto.SetTargetAllocationsRequest;
import com.wellsfargo.counselor.dto.TargetAllocationEntry;
import com.wellsfargo.counselor.entity.Advisor;
import com.wellsfargo.counselor.entity.Customer;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.PriceQuote;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.SecurityType;
import com.wellsfargo.counselor.entity.TradeSide;
import com.wellsfargo.counselor.fx.InMemoryExchangeRateProvider;
import com.wellsfargo.counselor.repository.AdvisorRepository;
import com.wellsfargo.counselor.repository.CustomerRepository;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import com.wellsfargo.counselor.repository.PositionRepository;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import com.wellsfargo.counselor.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Slice test for {@link RebalancingService}: plan generation and execution toward target weights. */
@DataJpaTest
@Import({RebalancingService.class, TradeService.class, InMemoryExchangeRateProvider.class, JpaAuditingConfig.class})
class RebalancingServiceTest {

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
    @Autowired
    private RebalancingService rebalancingService;
    @Autowired
    private TradeService tradeService;

    private Long portfolioId;
    private Long idA;
    private Long idB;

    @BeforeEach
    void setUp() {
        Advisor advisor = new Advisor("Ada", "Lovelace", "1 Analytical Way", "+1 555 0100", "ada@wf.test");
        Customer customer = new Customer("Grace", "Hopper", "grace@wf.test", "+1 555 0101", "2 Compiler Rd",
                LocalDate.of(1980, 1, 1));
        advisor.addCustomer(customer);
        advisorRepository.save(advisor);
        customerRepository.save(customer);

        Security a = securityRepository.save(new Security("AAA", "Alpha", SecurityType.COMMON_STOCK, "USD", "NYSE", null));
        Security b = securityRepository.save(new Security("BBB", "Beta", SecurityType.COMMON_STOCK, "USD", "NYSE", null));
        idA = a.getId();
        idB = b.getId();
        Portfolio portfolio = portfolioRepository.save(new Portfolio("Growth", "USD", customer));
        portfolioId = portfolio.getId();

        priceQuoteRepository.save(PriceQuote.of(a, LocalDate.parse("2026-06-15"), Money.of("100.00", "USD"), null));
        priceQuoteRepository.save(PriceQuote.of(b, LocalDate.parse("2026-06-15"), Money.of("50.00", "USD"), null));

        tradeService.execute(portfolioId, new ExecuteTradeRequest(idA, TradeSide.BUY,
                new BigDecimal("70"), new BigDecimal("100.00"), "USD", "init-a"));
        tradeService.execute(portfolioId, new ExecuteTradeRequest(idB, TradeSide.BUY,
                new BigDecimal("60"), new BigDecimal("50.00"), "USD", "init-b"));

        rebalancingService.setTargets(portfolioId, new SetTargetAllocationsRequest(List.of(
                new TargetAllocationEntry(idA, new BigDecimal("0.5")),
                new TargetAllocationEntry(idB, new BigDecimal("0.5")))));
    }

    @Test
    void planProposesTradesTowardEqualWeight() {
        RebalancePlan plan = rebalancingService.computePlan(portfolioId, BigDecimal.ZERO);

        assertThat(plan.totalValue()).isEqualByComparingTo("10000.00");
        RebalanceProposedTrade a = plan.proposedTrades().stream()
                .filter(t -> t.securityId().equals(idA)).findFirst().orElseThrow();
        RebalanceProposedTrade b = plan.proposedTrades().stream()
                .filter(t -> t.securityId().equals(idB)).findFirst().orElseThrow();
        assertThat(a.side()).isEqualTo(TradeSide.SELL);
        assertThat(a.quantity()).isEqualByComparingTo("20");
        assertThat(b.side()).isEqualTo(TradeSide.BUY);
        assertThat(b.quantity()).isEqualByComparingTo("40");
    }

    @Test
    void executeMovesHoldingsToTarget() {
        RebalanceResult result = rebalancingService.execute(portfolioId, BigDecimal.ZERO);

        assertThat(result.executedCount()).isEqualTo(2);
        BigDecimal quantityA = positionRepository.findByPortfolioIdAndSecurityId(portfolioId, idA).orElseThrow().getQuantity();
        BigDecimal quantityB = positionRepository.findByPortfolioIdAndSecurityId(portfolioId, idB).orElseThrow().getQuantity();
        assertThat(quantityA).isEqualByComparingTo("50");  // 70 - 20
        assertThat(quantityB).isEqualByComparingTo("100"); // 60 + 40
    }
}
