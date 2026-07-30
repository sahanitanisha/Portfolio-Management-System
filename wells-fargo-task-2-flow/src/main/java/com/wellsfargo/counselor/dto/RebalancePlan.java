package com.wellsfargo.counselor.dto;

import java.math.BigDecimal;
import java.util.List;

/** Preview of the trades a rebalance would generate, without executing them. */
public record RebalancePlan(
        Long portfolioId,
        String baseCurrency,
        BigDecimal totalValue,
        BigDecimal driftThreshold,
        List<RebalanceProposedTrade> proposedTrades
) {
}
