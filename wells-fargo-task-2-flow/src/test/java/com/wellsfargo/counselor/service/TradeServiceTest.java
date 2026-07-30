package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.dto.ExecuteTradeRequest;
import com.wellsfargo.counselor.dto.TradeResponse;
import com.wellsfargo.counselor.entity.Advisor;
import com.wellsfargo.counselor.entity.Customer;
import com.wellsfargo.counselor.entity.Portfolio;
import com.wellsfargo.counselor.entity.Position;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.SecurityType;
import com.wellsfargo.counselor.entity.TradeSide;
import com.wellsfargo.counselor.exception.InsufficientHoldingsException;
import com.wellsfargo.counselor.repository.AdvisorRepository;
import com.wellsfargo.counselor.repository.CustomerRepository;
import com.wellsfargo.counselor.repository.PortfolioRepository;
import com.wellsfargo.counselor.repository.PositionRepository;
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

/** Slice test for {@link TradeService}: position lifecycle, realised P&L, oversell/no-position, idempotency. */
@DataJpaTest
@Import({TradeService.class, JpaAuditingConfig.class})
class TradeServiceTest {

    @Autowired
    private AdvisorRepository advisorRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private SecurityRepository securityRepository;
    @Autowired
    private PortfolioRepository portfolioRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private TradeService tradeService;

    private Long portfolioId;
    private Long securityId;

    @BeforeEach
    void setUp() {
        Advisor advisor = new Advisor("Ada", "Lovelace", "1 Analytical Way", "+1 555 0100", "ada@wf.test");
        Customer customer = new Customer("Grace", "Hopper", "grace@wf.test", "+1 555 0101", "2 Compiler Rd",
                LocalDate.of(1980, 1, 1));
        advisor.addCustomer(customer);
        advisorRepository.save(advisor);
        customerRepository.save(customer);
        Security security = securityRepository.save(
                new Security("ACME", "Acme Corp", SecurityType.COMMON_STOCK, "USD", "NYSE", null));
        Portfolio portfolio = portfolioRepository.save(new Portfolio("Growth", "USD", customer));
        portfolioId = portfolio.getId();
        securityId = security.getId();
    }

    private ExecuteTradeRequest trade(TradeSide side, String quantity, String price, String reference) {
        return new ExecuteTradeRequest(securityId, side, new BigDecimal(quantity), new BigDecimal(price), "USD", reference);
    }

    private Position position() {
        return positionRepository.findByPortfolioIdAndSecurityId(portfolioId, securityId).orElseThrow();
    }

    @Test
    void buyCreatesPosition() {
        TradeResponse executed = tradeService.execute(portfolioId, trade(TradeSide.BUY, "10", "100.00", "b1"));

        assertThat(executed.side()).isEqualTo(TradeSide.BUY);
        assertThat(executed.notional()).isEqualByComparingTo("1000.00");
        assertThat(position().getQuantity()).isEqualByComparingTo("10");
        assertThat(position().getCostBasis().getAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void buyAgainRollsAverageCost() {
        tradeService.execute(portfolioId, trade(TradeSide.BUY, "10", "100.00", "b1"));
        tradeService.execute(portfolioId, trade(TradeSide.BUY, "10", "120.00", "b2"));

        assertThat(position().getQuantity()).isEqualByComparingTo("20");
        assertThat(position().getCostBasis().getAmount()).isEqualByComparingTo("2200.00");
    }

    @Test
    void sellReducesPositionAndReturnsRealisedPnl() {
        tradeService.execute(portfolioId, trade(TradeSide.BUY, "10", "100.00", "b1"));
        TradeResponse sell = tradeService.execute(portfolioId, trade(TradeSide.SELL, "4", "150.00", "s1"));

        assertThat(sell.realisedPnL()).isEqualByComparingTo("200.00"); // 4 * (150 - 100)
        assertThat(position().getQuantity()).isEqualByComparingTo("6");
        assertThat(position().getCostBasis().getAmount()).isEqualByComparingTo("600.00");
    }

    @Test
    void oversellIsRejected() {
        tradeService.execute(portfolioId, trade(TradeSide.BUY, "5", "100.00", "b1"));
        assertThatThrownBy(() -> tradeService.execute(portfolioId, trade(TradeSide.SELL, "6", "100.00", "s1")))
                .isInstanceOf(InsufficientHoldingsException.class);
    }

    @Test
    void sellWithoutPositionIsRejected() {
        assertThatThrownBy(() -> tradeService.execute(portfolioId, trade(TradeSide.SELL, "1", "100.00", "s1")))
                .isInstanceOf(InsufficientHoldingsException.class);
    }

    @Test
    void tradeExecutionIsIdempotentOnReference() {
        TradeResponse first = tradeService.execute(portfolioId, trade(TradeSide.BUY, "10", "100.00", "dup"));
        TradeResponse replay = tradeService.execute(portfolioId, trade(TradeSide.BUY, "10", "100.00", "dup"));

        assertThat(replay.id()).isEqualTo(first.id());          // same trade, not re-executed
        assertThat(position().getQuantity()).isEqualByComparingTo("10"); // quantity not doubled
    }
}
