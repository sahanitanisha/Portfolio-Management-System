package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Inbound payload to open an investment portfolio for a client. */
public record CreatePortfolioRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "base currency must be a 3-letter ISO-4217 code") String baseCurrency
) {
}
