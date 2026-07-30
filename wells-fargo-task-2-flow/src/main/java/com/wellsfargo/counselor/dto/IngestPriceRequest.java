package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inbound payload to ingest a daily close for a security. The quote currency is the security's trading
 * currency, so it is not supplied here.
 */
public record IngestPriceRequest(
        @NotNull @PastOrPresent LocalDate asOf,
        @NotNull @Positive BigDecimal close,
        Long volume
) {
}
