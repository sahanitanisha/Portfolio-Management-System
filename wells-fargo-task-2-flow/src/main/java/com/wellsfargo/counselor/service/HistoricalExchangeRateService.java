package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.dto.FxRateRequest;
import com.wellsfargo.counselor.dto.FxRateResponse;
import com.wellsfargo.counselor.entity.ExchangeRate;
import com.wellsfargo.counselor.entity.Money;
import com.wellsfargo.counselor.exception.ExchangeRateUnavailableException;
import com.wellsfargo.counselor.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Date-indexed currency conversion for historical analytics: it returns the rate in effect on (or most
 * recently before) a given date, so a portfolio value series uses period-correct FX rather than today's.
 * Same-currency conversion is a no-op; a direct rate is used as-is, its inverse is derived when only the
 * reverse pair is stored, and an unknown pair fails fast.
 */
@Service
@Transactional(readOnly = true)
public class HistoricalExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalExchangeRateService.class);
    private static final int INVERSE_SCALE = 10;

    private final ExchangeRateRepository exchangeRateRepository;

    public HistoricalExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /** Converts {@code source} into {@code targetCurrency} using the rate in effect on {@code asOf}. */
    public Money convert(Money source, String targetCurrency, LocalDate asOf) {
        if (source.getCurrency().equals(targetCurrency)) {
            return source;
        }
        BigDecimal rate = rateAsOf(source.getCurrency(), targetCurrency, asOf);
        return Money.of(source.getAmount().multiply(rate), targetCurrency);
    }

    public BigDecimal rateAsOf(String fromCurrency, String toCurrency, LocalDate asOf) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        Optional<ExchangeRate> direct = exchangeRateRepository
                .findTopByFromCurrencyAndToCurrencyAndAsOfLessThanEqualOrderByAsOfDesc(fromCurrency, toCurrency, asOf);
        if (direct.isPresent()) {
            return direct.get().getRate();
        }
        Optional<ExchangeRate> inverse = exchangeRateRepository
                .findTopByFromCurrencyAndToCurrencyAndAsOfLessThanEqualOrderByAsOfDesc(toCurrency, fromCurrency, asOf);
        if (inverse.isPresent()) {
            return BigDecimal.ONE.divide(inverse.get().getRate(), INVERSE_SCALE, RoundingMode.HALF_EVEN);
        }
        throw new ExchangeRateUnavailableException(
                "No exchange rate for " + fromCurrency + "->" + toCurrency + " as of " + asOf);
    }

    @Transactional
    public FxRateResponse recordRate(FxRateRequest request) {
        ExchangeRate rate = exchangeRateRepository
                .findByFromCurrencyAndToCurrencyAndAsOf(request.fromCurrency(), request.toCurrency(), request.asOf())
                .orElseGet(() -> exchangeRateRepository.save(ExchangeRate.of(
                        request.fromCurrency(), request.toCurrency(), request.asOf(), request.rate())));
        return FxRateResponse.from(rate);
    }

    @Transactional
    public List<FxRateResponse> recordRates(List<FxRateRequest> requests) {
        List<FxRateResponse> results = new ArrayList<>(requests.size());
        for (FxRateRequest request : requests) {
            results.add(recordRate(request));
        }
        log.info("Ingested {} historical FX rate(s)", requests.size());
        return results;
    }
}
