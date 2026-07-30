package com.wellsfargo.counselor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Inbound payload for bulk price ingestion. {@code @Valid} cascades validation to each entry. */
public record IngestPricesRequest(
        @NotEmpty @Valid List<IngestPriceRequest> prices
) {
}
