package com.wellsfargo.counselor.rebalance;

import com.wellsfargo.counselor.dto.RebalanceProposedTrade;
import com.wellsfargo.counselor.entity.TradeSide;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure rebalancing algorithm. Given each holding's current value, price and target weight (all in base
 * currency), and the portfolio's total value, it produces the trades needed to move each holding toward
 * {@code totalValue x targetWeight}.
 *
 * <p>A holding is skipped when it has no usable price or when its drift is within {@code driftThreshold}
 * of total value (avoids churn on tiny deviations). Sell quantities are capped at the amount held, and
 * quantities are rounded down so a rebalance never overshoots a target.
 */
public final class RebalancingCalculator {

    private static final int QUANTITY_SCALE = 6;
    private static final int VALUE_SCALE = 2;

    private RebalancingCalculator() {
    }

    public static List<RebalanceProposedTrade> plan(List<RebalanceInput> lines, BigDecimal totalValue,
                                                    BigDecimal driftThreshold) {
        List<RebalanceProposedTrade> trades = new ArrayList<>();
        if (totalValue == null || totalValue.signum() <= 0) {
            return trades;
        }
        BigDecimal threshold = (driftThreshold == null) ? BigDecimal.ZERO : driftThreshold;
        BigDecimal toleranceAmount = totalValue.multiply(threshold);

        for (RebalanceInput line : lines) {
            if (line.priceBase() == null || line.priceBase().signum() <= 0) {
                continue; // cannot size a trade without a price
            }
            BigDecimal targetValue = totalValue.multiply(line.targetWeight()).setScale(VALUE_SCALE, RoundingMode.HALF_EVEN);
            BigDecimal currentValue = nullToZero(line.currentValueBase()).setScale(VALUE_SCALE, RoundingMode.HALF_EVEN);
            BigDecimal delta = targetValue.subtract(currentValue);
            if (delta.abs().compareTo(toleranceAmount) <= 0) {
                continue; // within drift tolerance
            }

            TradeSide side = (delta.signum() > 0) ? TradeSide.BUY : TradeSide.SELL;
            BigDecimal quantity = delta.abs().divide(line.priceBase(), QUANTITY_SCALE, RoundingMode.DOWN);
            if (side == TradeSide.SELL) {
                quantity = quantity.min(nullToZero(line.heldQuantity()));
            }
            if (quantity.signum() <= 0) {
                continue;
            }
            trades.add(new RebalanceProposedTrade(line.securityId(), line.symbol(), side, quantity,
                    currentValue, targetValue, delta));
        }
        return trades;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return (value == null) ? BigDecimal.ZERO : value;
    }
}
