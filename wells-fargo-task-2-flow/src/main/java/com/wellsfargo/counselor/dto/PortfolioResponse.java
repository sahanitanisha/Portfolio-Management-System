package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.Portfolio;

/** Outbound representation of a portfolio. */
public record PortfolioResponse(
        Long id,
        String name,
        String baseCurrency,
        Long customerId
) {
    public static PortfolioResponse from(Portfolio portfolio) {
        return new PortfolioResponse(portfolio.getId(), portfolio.getName(), portfolio.getBaseCurrency(),
                portfolio.getCustomer() != null ? portfolio.getCustomer().getId() : null);
    }
}
