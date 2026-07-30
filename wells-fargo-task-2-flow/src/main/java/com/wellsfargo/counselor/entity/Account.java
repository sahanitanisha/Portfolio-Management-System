package com.wellsfargo.counselor.entity;

import com.wellsfargo.counselor.exception.InsufficientFundsException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A customer account holding a currency-denominated {@link Money} balance and an append-only ledger
 * of {@link Transaction}s. Balance state is derived exclusively from posted transactions via
 * {@link #recordTransaction}, so the in-memory balance and the ledger can never diverge.
 */
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    @NotBlank
    @Column(name = "account_number", unique = true, nullable = false, updatable = false)
    private String accountNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "balance_currency", nullable = false, length = 3))
    })
    private Money balance;

    /**
     * Annual interest rate expressed as a decimal fraction (e.g. {@code 0.0325} for 3.25%), never a
     * percentage. {@code precision = 9, scale = 6} supports rates to a millionth without overflow.
     */
    @NotNull
    @DecimalMin(value = "0.0", message = "interest rate must not be negative")
    @Column(name = "interest_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal interestRate;

    @NotNull
    @Column(name = "opened_on", nullable = false, updatable = false)
    private LocalDate openedOn;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    /** Required by JPA. */
    protected Account() {
    }

    /**
     * Opens an account with a zero balance in the given ISO-4217 currency. The currency is fixed for
     * the life of the account; every transaction posted to it must match.
     */
    public Account(String accountNumber, AccountType accountType, BigDecimal interestRate,
                   LocalDate openedOn, String currencyCode) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.interestRate = (interestRate != null) ? interestRate : BigDecimal.ZERO;
        this.openedOn = (openedOn != null) ? openedOn : LocalDate.now();
        this.balance = Money.zero(currencyCode);
        this.active = true;
    }

    /**
     * Posts an entry to the ledger and atomically updates the balance. The {@link TransactionType}
     * determines direction; {@code amount} is always a non-negative magnitude. Currency consistency
     * is enforced by {@link Money}; a debit that would overdraw is rejected.
     *
     * @return the immutable {@link Transaction} that was recorded
     */
    public Transaction recordTransaction(TransactionType type, Money amount, String description,
                                         String referenceNumber) {
        Objects.requireNonNull(type, "transaction type must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.isNegative()) {
            throw new IllegalArgumentException(
                    "Amount must be a non-negative magnitude; direction is set by the transaction type");
        }
        if (type.isCredit()) {
            this.balance = this.balance.add(amount);
        } else if (type.isDebit()) {
            Money next = this.balance.subtract(amount);
            if (next.isNegative()) {
                throw new InsufficientFundsException(
                        "Insufficient funds for " + type + " on account " + accountNumber);
            }
            this.balance = next;
        }
        Transaction transaction = Transaction.post(this, type, amount, this.balance, description, referenceNumber);
        this.transactions.add(transaction);
        return transaction;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    /** Balance is read-only; change it only by posting transactions through {@link #recordTransaction}. */
    public Money getBalance() {
        return balance;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Customer getCustomer() {
        return customer;
    }

    /** Package-private: the customer link is managed by {@link Customer#addAccount}/{@link Customer#removeAccount}. */
    void setCustomer(Customer customer) {
        this.customer = customer;
    }

    /** Read-only view of the ledger; entries are added only via {@link #recordTransaction}. */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }
}
