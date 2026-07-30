package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.SecurityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Inbound payload to register a tradable instrument. */
public record CreateSecurityRequest(
        @NotBlank @Size(max = 32) String symbol,
        @NotBlank String name,
        @NotNull SecurityType type,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO-4217 code") String currency,
        @Size(max = 32) String exchange,
        @Size(min = 12, max = 12, message = "ISIN must be 12 characters") String isin
) {
}
