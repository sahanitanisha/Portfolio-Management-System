package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    Page<Account> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Loads an account under a {@code PESSIMISTIC_WRITE} (row) lock. Used on the balance-mutation
     * path so concurrent posts to the same account are serialised by the database, eliminating the
     * lost-update race deterministically rather than relying on optimistic retry for a hot row.
     * Must be invoked inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
