package com.wellsfargo.counselor.rebalance;

import com.wellsfargo.counselor.dto.RebalanceProposedTrade;
import com.wellsfargo.counselor.entity.TradeSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the rebalancing algorithm against hand-computed targets. */
class RebalancingCalculatorTest {

    private RebalanceInput line(long id, String symbol, String currentValue, String price, String held, String weight) {
        return new RebalanceInput(id, symbol, new BigDecimal(currentValue), new BigDecimal(price),
                new BigDecimal(held), new BigDecimal(weight));
    }

    @Test
    void generatesBuyAndSellTowardTargets() {
        List<RebalanceInput> lines = List.of(
                line(1, "A", "7000", "100", "70", "0.5"),   // target 5000 -> sell 2000 -> 20 sh
                line(2, "B", "3000", "50", "60", "0.5"));    // target 5000 -> buy 2000 -> 40 sh
        List<RebalanceProposedTrade> trades = RebalancingCalculator.plan(lines, new BigDecimal("10000"), BigDecimal.ZERO);

        assertThat(trades).hasSize(2);
        RebalanceProposedTrade a = trades.stream().filter(t -> t.securityId().equals(1L)).findFirst().orElseThrow();
        RebalanceProposedTrade b = trades.stream().filter(t -> t.securityId().equals(2L)).findFirst().orElseThrow();
        assertThat(a.side()).isEqualTo(TradeSide.SELL);
        assertThat(a.quantity()).isEqualByComparingTo("20");
        assertThat(b.side()).isEqualTo(TradeSide.BUY);
        assertThat(b.quantity()).isEqualByComparingTo("40");
    }

    @Test
    void skipsTradesWithinDriftTolerance() {
        List<RebalanceInput> lines = List.of(
                line(1, "A", "7000", "100", "70", "0.5"),
                line(2, "B", "3000", "50", "60", "0.5"));
        // tolerance = 25% of 10000 = 2500 > drift of 2000 -> nothing to do
        List<RebalanceProposedTrade> trades = RebalancingCalculator.plan(lines, new BigDecimal("10000"), new BigDecimal("0.25"));

        assertThat(trades).isEmpty();
    }

    @Test
    void capsSellAtHeldQuantityAndSkipsUnpriced() {
        List<RebalanceInput> lines = List.of(
                line(1, "A", "10000", "100", "5", "0.0"),    // sell everything, but only 5 held
                new RebalanceInput(2L, "B", new BigDecimal("0"), null, BigDecimal.ZERO, new BigDecimal("1.0"))); // no price
        List<RebalanceProposedTrade> trades = RebalancingCalculator.plan(lines, new BigDecimal("10000"), BigDecimal.ZERO);

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).securityId()).isEqualTo(1L);
        assertThat(trades.get(0).side()).isEqualTo(TradeSide.SELL);
        assertThat(trades.get(0).quantity()).isEqualByComparingTo("5");
    }
}
