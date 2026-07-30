package com.wellsfargo.counselor.entity;

/**
 * Classifies a ledger entry and encodes its effect on the account balance via a fixed direction
 * ({@code +1} credit, {@code -1} debit, {@code 0} balance-neutral). Centralising direction here keeps
 * posting logic in one place and prevents callers from inventing their own sign conventions.
 */
public enum TransactionType {
    DEPOSIT(1),
    INTEREST(1),
    DIVIDEND(1),
    TRANSFER_IN(1),
    WITHDRAWAL(-1),
    FEE(-1),
    TRANSFER_OUT(-1),
    ADJUSTMENT(0),
    REVERSAL(0);

    private final int direction;

    TransactionType(int direction) {
        this.direction = direction;
    }

    public boolean isCredit() {
        return direction > 0;
    }

    public boolean isDebit() {
        return direction < 0;
    }

    public int direction() {
        return direction;
    }
}
