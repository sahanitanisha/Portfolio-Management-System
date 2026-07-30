package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /** Most recent rate for the pair on or before the given date (period-correct lookup). */
    Optional<ExchangeRate> findTopByFromCurrencyAndToCurrencyAndAsOfLessThanEqualOrderByAsOfDesc(
            String fromCurrency, String toCurrency, LocalDate asOf);

    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndAsOf(
            String fromCurrency, String toCurrency, LocalDate asOf);
}
