package com.wellsfargo.counselor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.SecurityType;

/** Outbound representation of a security (reference data). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SecurityResponse(
        Long id,
        String symbol,
        String name,
        SecurityType type,
        String currency,
        String exchange,
        String isin
) {
    public static SecurityResponse from(Security security) {
        return new SecurityResponse(security.getId(), security.getSymbol(), security.getName(),
                security.getType(), security.getCurrency(), security.getExchange(), security.getIsin());
    }
}
