package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    /** Idempotency lookup for trade execution. */
    Optional<Trade> findByReferenceNumber(String referenceNumber);

    /** Trade blotter, newest first. Backed by the {@code idx_trade_portfolio}/{@code executed_at} indexes. */
    Page<Trade> findByPortfolioIdOrderByExecutedAtDesc(Long portfolioId, Pageable pageable);

    /** Full trade history (oldest first), securities fetched, for as-of holdings reconstruction. */
    @EntityGraph(attributePaths = "security")
    List<Trade> findByPortfolioIdOrderByExecutedAtAsc(Long portfolioId);
}
