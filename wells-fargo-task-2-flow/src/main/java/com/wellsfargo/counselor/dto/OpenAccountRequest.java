package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Inbound payload to open an account for a client. Currency is fixed at open time. */
public record OpenAccountRequest(
        @NotBlank String accountNumber,
        @NotNull AccountType accountType,
        @NotNull @DecimalMin(value = "0.0", message = "interest rate must not be negative") BigDecimal interestRate,
        @NotBlank @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO-4217 code") String currencyCode,
        LocalDate openedOn
) {
}
