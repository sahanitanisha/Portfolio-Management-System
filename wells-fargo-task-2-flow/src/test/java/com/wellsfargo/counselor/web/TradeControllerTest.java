package com.wellsfargo.counselor.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellsfargo.counselor.dto.ExecuteTradeRequest;
import com.wellsfargo.counselor.dto.TradeResponse;
import com.wellsfargo.counselor.entity.TradeSide;
import com.wellsfargo.counselor.exception.InsufficientHoldingsException;
import com.wellsfargo.counselor.security.JwtService;
import com.wellsfargo.counselor.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer slice test for {@link TradeController}: mapping, validation, and exception translation. */
@WebMvcTest(TradeController.class)
@AutoConfigureMockMvc(addFilters = false)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private TradeService tradeService;
    @MockBean
    private JwtService jwtService;

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void executeReturnsCreated() throws Exception {
        TradeResponse response = new TradeResponse(1L, 1L, 1L, "ACME", TradeSide.BUY,
                new BigDecimal("10"), new BigDecimal("100.00"), "USD", new BigDecimal("1000.00"),
                null, Instant.parse("2026-06-19T10:00:00Z"), "t-1");
        given(tradeService.execute(eq(1L), any())).willReturn(response);

        mockMvc.perform(post("/api/v1/portfolios/1/trades").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ExecuteTradeRequest(1L, TradeSide.BUY,
                                new BigDecimal("10"), new BigDecimal("100.00"), "USD", "t-1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.symbol").value("ACME"));
    }

    @Test
    void negativeQuantityIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/portfolios/1/trades").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ExecuteTradeRequest(1L, TradeSide.BUY,
                                new BigDecimal("-5"), new BigDecimal("100.00"), "USD", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientHoldingsMapsToUnprocessableEntity() throws Exception {
        given(tradeService.execute(any(), any())).willThrow(new InsufficientHoldingsException("only 5 held"));

        mockMvc.perform(post("/api/v1/portfolios/1/trades").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ExecuteTradeRequest(1L, TradeSide.SELL,
                                new BigDecimal("9999"), new BigDecimal("100.00"), "USD", null))))
                .andExpect(status().isUnprocessableEntity());
    }
}
