package com.wellsfargo.counselor.dto;

import java.util.List;

/** Outcome of executing a rebalance: the trades actually placed. */
public record RebalanceResult(
        Long portfolioId,
        int executedCount,
        List<TradeResponse> executedTrades
) {
}
