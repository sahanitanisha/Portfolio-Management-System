package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.Transaction;
import com.wellsfargo.counselor.entity.TransactionStatus;
import com.wellsfargo.counselor.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/** Outbound representation of an immutable ledger entry. */
public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String currency,
        BigDecimal balanceAfter,
        String balanceAfterCurrency,
        String description,
        Instant executedAt,
        String referenceNumber,
        TransactionStatus status,
        Long accountId
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getAmount().getAmount(),
                tx.getAmount().getCurrency(),
                tx.getBalanceAfter() != null ? tx.getBalanceAfter().getAmount() : null,
                tx.getBalanceAfter() != null ? tx.getBalanceAfter().getCurrency() : null,
                tx.getDescription(),
                tx.getExecutedAt(),
                tx.getReferenceNumber(),
                tx.getStatus(),
                tx.getAccount() != null ? tx.getAccount().getId() : null);
    }
}
