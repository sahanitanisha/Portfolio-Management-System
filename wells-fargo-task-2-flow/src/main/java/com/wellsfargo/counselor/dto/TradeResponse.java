package com.wellsfargo.counselor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wellsfargo.counselor.entity.Trade;
import com.wellsfargo.counselor.entity.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

/** Outbound representation of an executed trade. {@code realisedPnL} is present for sells only. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TradeResponse(
        Long id,
        Long portfolioId,
        Long securityId,
        String symbol,
        TradeSide side,
        BigDecimal quantity,
        BigDecimal price,
        String currency,
        BigDecimal notional,
        BigDecimal realisedPnL,
        Instant executedAt,
        String referenceNumber
) {
    public static TradeResponse from(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getPortfolio().getId(),
                trade.getSecurity().getId(),
                trade.getSecurity().getSymbol(),
                trade.getSide(),
                trade.getQuantity(),
                trade.getPricePerShare().getAmount(),
                trade.getPricePerShare().getCurrency(),
                trade.notional().getAmount(),
                trade.getRealisedPnL() != null ? trade.getRealisedPnL().getAmount() : null,
                trade.getExecutedAt(),
                trade.getReferenceNumber());
    }
}
