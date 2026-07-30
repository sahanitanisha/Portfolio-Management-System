package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Idempotency lookup: a non-null reference number uniquely identifies a posted entry. */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /** Account statement, newest first. Backed by the {@code idx_tx_account}/{@code executed_at} indexes. */
    Page<Transaction> findByAccountIdOrderByExecutedAtDesc(Long accountId, Pageable pageable);
}
