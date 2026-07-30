package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.FxRateRequest;
import com.wellsfargo.counselor.dto.FxRateResponse;
import com.wellsfargo.counselor.dto.FxRatesRequest;
import com.wellsfargo.counselor.service.HistoricalExchangeRateService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fx-rates")
public class FxRateController {

    private final HistoricalExchangeRateService exchangeRateService;

    public FxRateController(HistoricalExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @PostMapping
    public ResponseEntity<FxRateResponse> record(@Valid @RequestBody FxRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exchangeRateService.recordRate(request));
    }

    @PostMapping("/batch")
    public List<FxRateResponse> recordBatch(@Valid @RequestBody FxRatesRequest request) {
        return exchangeRateService.recordRates(request.rates());
    }

    /** Resolves the rate in effect on a date (uses the most recent rate on or before it). */
    @GetMapping("/rate")
    public FxRateResponse rateAsOf(@RequestParam String from, @RequestParam String to,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return new FxRateResponse(from, to, asOf, exchangeRateService.rateAsOf(from, to, asOf));
    }
}
