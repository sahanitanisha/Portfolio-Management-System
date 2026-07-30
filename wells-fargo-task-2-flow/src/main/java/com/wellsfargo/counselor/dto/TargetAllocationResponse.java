package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.TargetAllocation;

import java.math.BigDecimal;

/** Outbound representation of a stored target allocation. */
public record TargetAllocationResponse(
        Long securityId,
        String symbol,
        BigDecimal weight
) {
    public static TargetAllocationResponse from(TargetAllocation allocation) {
        return new TargetAllocationResponse(
                allocation.getSecurity().getId(),
                allocation.getSecurity().getSymbol(),
                allocation.getTargetWeight());
    }
}
