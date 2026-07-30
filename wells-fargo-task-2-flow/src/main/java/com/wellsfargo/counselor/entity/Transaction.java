package com.wellsfargo.counselor.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable, append-only ledger entry.
 *
 * <p>For auditability every field is write-once: there are no setters and every column is mapped
 * {@code updatable = false}, so once an entry is persisted neither application code nor an accidental
 * {@code merge} can alter it. Corrections are modelled as new compensating entries
 * ({@link TransactionType#REVERSAL}), never by editing history. Instances are created only via the
 * package-private {@link #post} factory, invoked by {@link Account#recordTransaction}, which guarantees
 * the balance and the ledger move together.
 *
 * <p>Indexes on {@code account_id} and {@code executed_at} support the common "statement for an
 * account over a date range" query without a full scan.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_account", columnList = "account_id"),
        @Index(name = "idx_tx_executed_at", columnList = "executed_at")
})
public class Transaction extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false)
    private TransactionType type;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "amount_value", nullable = false, updatable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "amount_currency", nullable = false, updatable = false, length = 3))
    })
    private Money amount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "balance_after_value", updatable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "balance_after_currency", updatable = false, length = 3))
    })
    private Money balanceAfter;

    @Column(name = "description", updatable = false, length = 500)
    private String description;

    @NotNull
    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    @Column(name = "reference_number", unique = true, updatable = false, length = 64)
    private String referenceNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false)
    private TransactionStatus status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    /** Required by JPA. */
    protected Transaction() {
    }

    private Transaction(Account account, TransactionType type, Money amount, Money balanceAfter,
                        String description, String referenceNumber, Instant executedAt,
                        TransactionStatus status) {
        this.account = Objects.requireNonNull(account, "account must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.referenceNumber = referenceNumber;
        this.executedAt = (executedAt != null) ? executedAt : Instant.now();
        this.status = (status != null) ? status : TransactionStatus.COMPLETED;
    }

    /**
     * Records a completed entry against {@code account}. Package-private by design: posting must go
     * through {@link Account#recordTransaction} so the balance snapshot ({@code balanceAfter}) is
     * always consistent with the entry.
     */
    static Transaction post(Account account, TransactionType type, Money amount, Money balanceAfter,
                            String description, String referenceNumber) {
        return new Transaction(account, type, amount, balanceAfter, description, referenceNumber,
                Instant.now(), TransactionStatus.COMPLETED);
    }

    public TransactionType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Account getAccount() {
        return account;
    }
}
