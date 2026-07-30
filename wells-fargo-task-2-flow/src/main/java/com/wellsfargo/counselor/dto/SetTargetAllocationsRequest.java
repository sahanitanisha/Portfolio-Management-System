package com.wellsfargo.counselor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Inbound payload to replace a portfolio's full set of target allocations. */
public record SetTargetAllocationsRequest(
        @NotEmpty @Valid List<TargetAllocationEntry> allocations
) {
}
