package com.wellsfargo.counselor.entity;

/**
 * Lifecycle state of a ledger entry. Because the ledger is append-only, a state change is recorded
 * as a new {@code REVERSED}/correcting entry rather than by mutating an existing row.
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REVERSED
}
