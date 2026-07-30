package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.PriceQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceQuoteRepository extends JpaRepository<PriceQuote, Long> {

    /** Most recent close, used for current valuation. */
    Optional<PriceQuote> findFirstBySecurityIdOrderByAsOfDesc(Long securityId);

    Optional<PriceQuote> findBySecurityIdAndAsOf(Long securityId, LocalDate asOf);

    boolean existsBySecurityIdAndAsOf(Long securityId, LocalDate asOf);

    /** Ascending price history feeding return / volatility / drawdown calculations. */
    List<PriceQuote> findBySecurityIdAndAsOfBetweenOrderByAsOfAsc(Long securityId, LocalDate from, LocalDate to);
}
