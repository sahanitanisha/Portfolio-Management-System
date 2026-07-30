package com.wellsfargo.counselor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Inbound payload for bulk historical FX rate ingestion. */
public record FxRatesRequest(
        @NotEmpty @Valid List<FxRateRequest> rates
) {
}
