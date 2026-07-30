package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.ExecuteTradeRequest;
import com.wellsfargo.counselor.dto.TradeResponse;
import com.wellsfargo.counselor.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> execute(@PathVariable Long portfolioId,
                                                 @Valid @RequestBody ExecuteTradeRequest request) {
        TradeResponse executed = tradeService.execute(portfolioId, request);
        return ResponseEntity.created(URI.create("/api/v1/trades/" + executed.id())).body(executed);
    }

    @GetMapping
    public Page<TradeResponse> blotter(@PathVariable Long portfolioId, Pageable pageable) {
        return tradeService.blotter(portfolioId, pageable);
    }
}
