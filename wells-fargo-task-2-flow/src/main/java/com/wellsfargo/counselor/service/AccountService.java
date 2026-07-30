package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.AccountResponse;
import com.wellsfargo.counselor.dto.OpenAccountRequest;
import com.wellsfargo.counselor.dto.PostTransactionRequest;
import com.wellsfargo.counselor.dto.TransactionResponse;
import com.wellsfargo.counselor.entity.Account;
import com.wellsfargo.counselor.entity.Customer;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.entity.Transaction;
import com.wellsfargo.counselor.exception.ResourceNotFoundException;
import com.wellsfargo.counselor.repository.AccountRepository;
import com.wellsfargo.counselor.repository.CustomerRepository;
import com.wellsfargo.counselor.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Application service for accounts and ledger posting.
 *
 * <p>{@link #postTransaction} is the integrity-critical path. It is idempotent on
 * {@code referenceNumber} (a replay returns the original entry, never double-posts) and takes a
 * {@code PESSIMISTIC_WRITE} lock on the account row so concurrent posts to the same balance are
 * serialised by the database. The unique constraint on {@code reference_number} is the final backstop
 * against a race, surfaced via {@link DataIntegrityViolationException} and resolved to the winning entry.
 */
@Service
@Transactional(readOnly = true)
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse openAccount(Long customerId, OpenAccountRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
        if (accountRepository.existsByAccountNumber(request.accountNumber())) {
            throw new IllegalArgumentException("Account number already exists: " + request.accountNumber());
        }
        Account account = new Account(request.accountNumber(), request.accountType(),
                request.interestRate(), request.openedOn(), request.currencyCode());
        customer.addAccount(account); // keeps both sides of the association consistent
        Account saved = accountRepository.save(account);
        log.info("Opened account {} (id={}) for customer id={}",
                saved.getAccountNumber(), saved.getId(), customerId);
        return AccountResponse.from(saved);
    }

    public AccountResponse getAccount(Long id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", id));
    }

    public Page<AccountResponse> listByCustomer(Long customerId, Pageable pageable) {
        return accountRepository.findByCustomerId(customerId, pageable).map(AccountResponse::from);
    }

    public Page<TransactionResponse> statement(Long accountId, Pageable pageable) {
        return transactionRepository.findByAccountIdOrderByExecutedAtDesc(accountId, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional
    public TransactionResponse postTransaction(Long accountId, PostTransactionRequest request) {
        // 1. Idempotent replay: a known reference returns the original entry, never double-posts.
        if (hasReference(request)) {
            Optional<Transaction> replay = transactionRepository.findByReferenceNumber(request.referenceNumber());
            if (replay.isPresent()) {
                log.info("Idempotent replay for reference={}", request.referenceNumber());
                return TransactionResponse.from(replay.get());
            }
        }
        // 2. Lock the balance row for the duration of the transaction.
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", accountId));
        Money amount = Money.of(request.amount(), request.currency());
        try {
            Transaction tx = account.recordTransaction(request.type(), amount,
                    request.description(), request.referenceNumber());
            accountRepository.saveAndFlush(account); // force insert to surface unique-reference races now
            log.info("Posted {} {} to account id={} (tx id={})", request.type(), amount, accountId, tx.getId());
            return TransactionResponse.from(tx);
        } catch (DataIntegrityViolationException ex) {
            // 3. Backstop: a concurrent insert won the unique-reference race - return the winner.
            if (hasReference(request)) {
                return transactionRepository.findByReferenceNumber(request.referenceNumber())
                        .map(TransactionResponse::from)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    private static boolean hasReference(PostTransactionRequest request) {
        return request.referenceNumber() != null && !request.referenceNumber().isBlank();
    }
}
