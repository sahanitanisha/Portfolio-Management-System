package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Inbound payload to post a ledger entry. {@code amount} is always a positive magnitude; the
 * {@link TransactionType} decides direction. {@code referenceNumber} is the optional idempotency key.
 */
public record PostTransactionRequest(
        @NotNull TransactionType type,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO-4217 code") String currency,
        @Size(max = 500) String description,
        @Size(max = 64) String referenceNumber
) {
}
