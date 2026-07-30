package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Outbound representation of a historical FX rate (stored or resolved as-of a date). */
public record FxRateResponse(
        String fromCurrency,
        String toCurrency,
        LocalDate asOf,
        BigDecimal rate
) {
    public static FxRateResponse from(ExchangeRate rate) {
        return new FxRateResponse(rate.getFromCurrency(), rate.getToCurrency(), rate.getAsOf(), rate.getRate());
    }
}
