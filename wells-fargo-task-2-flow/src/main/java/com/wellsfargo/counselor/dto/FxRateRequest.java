package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Inbound payload to record a historical FX rate ({@code toCurrency} units per one {@code fromCurrency}). */
public record FxRateRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO-4217 code") String fromCurrency,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO-4217 code") String toCurrency,
        @NotNull @PastOrPresent LocalDate asOf,
        @NotNull @Positive BigDecimal rate
) {
}
