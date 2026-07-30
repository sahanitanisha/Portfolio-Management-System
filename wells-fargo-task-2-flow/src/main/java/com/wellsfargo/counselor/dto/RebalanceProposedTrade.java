package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.TradeSide;

import java.math.BigDecimal;

/** A single proposed trade to move a holding toward its target weight (base-currency figures). */
public record RebalanceProposedTrade(
        Long securityId,
        String symbol,
        TradeSide side,
        BigDecimal quantity,
        BigDecimal currentValue,
        BigDecimal targetValue,
        BigDecimal delta
) {
}
