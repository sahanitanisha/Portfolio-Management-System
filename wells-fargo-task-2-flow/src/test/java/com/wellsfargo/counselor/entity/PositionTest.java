package com.wellsfargo.counselor.entity;

import com.wellsfargo.counselor.exception.InsufficientHoldingsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure-unit tests for {@link Position}'s average-cost and realised-P&L math (no persistence). */
class PositionTest {

    private Position newPosition(String quantity, String costPerShare) {
        Security security = new Security("ACME", "Acme Corp", SecurityType.COMMON_STOCK, "USD", "NYSE", null);
        Portfolio portfolio = new Portfolio("Growth", "USD", null);
        BigDecimal qty = new BigDecimal(quantity);
        Money totalCost = Money.of(costPerShare, "USD").multiply(qty);
        return new Position(portfolio, security, qty, totalCost);
    }

    @Test
    void buyRollsAverageCostForward() {
        Position pos = newPosition("10", "100.00");            // 10 @ 100 -> cost 1000
        pos.applyBuy(new BigDecimal("10"), Money.of("120.00", "USD")); // +10 @ 120 -> cost +1200

        assertThat(pos.getQuantity()).isEqualByComparingTo("20");
        assertThat(pos.getCostBasis().getAmount()).isEqualByComparingTo("2200.00");
        assertThat(pos.averageCostPerShare()).isEqualByComparingTo("110.00"); // (1000+1200)/20
    }

    @Test
    void sellReducesQuantityAndReturnsRealisedPnl() {
        Position pos = newPosition("10", "100.00");            // cost 1000, avg 100
        Money realised = pos.applySell(new BigDecimal("4"), Money.of("150.00", "USD"));

        assertThat(realised.getAmount()).isEqualByComparingTo("200.00"); // 4 * (150 - 100)
        assertThat(pos.getQuantity()).isEqualByComparingTo("6");
        assertThat(pos.getCostBasis().getAmount()).isEqualByComparingTo("600.00"); // 6 * 100
    }

    @Test
    void closingPositionZeroesCostBasis() {
        Position pos = newPosition("5", "100.00");
        pos.applySell(new BigDecimal("5"), Money.of("130.00", "USD"));

        assertThat(pos.getQuantity()).isEqualByComparingTo("0");
        assertThat(pos.getCostBasis().getAmount()).isEqualByComparingTo("0.00");
        assertThat(pos.averageCostPerShare()).isEqualByComparingTo("0");
    }

    @Test
    void sellingMoreThanHeldIsRejected() {
        Position pos = newPosition("5", "100.00");
        assertThatThrownBy(() -> pos.applySell(new BigDecimal("6"), Money.of("100.00", "USD")))
                .isInstanceOf(InsufficientHoldingsException.class);
    }

    @Test
    void marketValueAndUnrealisedGainReflectPrice() {
        Position pos = newPosition("10", "100.00");            // cost 1000
        Money price = Money.of("130.00", "USD");

        assertThat(pos.marketValue(price).getAmount()).isEqualByComparingTo("1300.00");
        assertThat(pos.unrealisedGain(price).getAmount()).isEqualByComparingTo("300.00");
    }
}
