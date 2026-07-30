package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.Portfolio;
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
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Page<Portfolio> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Loads a portfolio under a {@code PESSIMISTIC_WRITE} lock for the trade-application path, so
     * concurrent buys/sells against the same portfolio are serialised. Must run inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portfolio p where p.id = :id")
    Optional<Portfolio> findByIdForUpdate(@Param("id") Long id);
}
