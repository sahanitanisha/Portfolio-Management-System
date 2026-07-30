package com.wellsfargo.counselor.event;

/** Published after a price quote is ingested for a security, to trigger live valuation updates. */
public record PriceIngestedEvent(Long securityId) {
}
