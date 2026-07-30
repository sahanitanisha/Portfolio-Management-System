package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** One desired allocation: a target weight (fraction in [0,1]) for a security. */
public record TargetAllocationEntry(
        @NotNull Long securityId,
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") BigDecimal weight
) {
}
