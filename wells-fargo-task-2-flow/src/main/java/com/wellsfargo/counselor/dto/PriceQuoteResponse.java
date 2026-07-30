package com.wellsfargo.counselor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wellsfargo.counselor.entity.PriceQuote;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Outbound representation of a stored price quote. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriceQuoteResponse(
        Long id,
        Long securityId,
        String symbol,
        LocalDate asOf,
        BigDecimal close,
        String currency,
        Long volume
) {
    public static PriceQuoteResponse from(PriceQuote quote) {
        return new PriceQuoteResponse(quote.getId(), quote.getSecurity().getId(), quote.getSecurity().getSymbol(),
                quote.getAsOf(), quote.getClose().getAmount(), quote.getClose().getCurrency(), quote.getVolume());
    }
}
