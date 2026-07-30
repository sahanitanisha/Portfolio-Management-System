package com.wellsfargo.counselor.rebalance;

import java.math.BigDecimal;

/**
 * One line of input to the rebalancing calculator, all monetary figures in the portfolio's base
 * currency. {@code priceBase} is the security's price per share converted to base (null/zero if no
 * price is available, in which case the line is skipped).
 */
public record RebalanceInput(
        Long securityId,
        String symbol,
        BigDecimal currentValueBase,
        BigDecimal priceBase,
        BigDecimal heldQuantity,
        BigDecimal targetWeight
) {
}
