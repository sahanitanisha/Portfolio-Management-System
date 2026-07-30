package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.TradeSide;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Inbound payload to execute a trade. {@code price} is the per-share execution price in
 * {@code currency}, which must match the security's trading currency. {@code referenceNumber} is the
 * optional idempotency key.
 */
public record ExecuteTradeRequest(
        @NotNull Long securityId,
        @NotNull TradeSide side,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal price,
        @NotNull @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO-4217 code") String currency,
        @Size(max = 64) String referenceNumber
) {
}
