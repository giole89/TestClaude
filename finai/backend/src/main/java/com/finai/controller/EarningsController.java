package com.finai.controller;

import com.finai.dto.earnings.EarningsDto;
import com.finai.service.EarningsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Endpoint per le date di earnings dei titoli.
 */
@RestController
@RequestMapping("/api/earnings")
@Tag(name = "Earnings", description = "Date e stime earnings per ticker")
public class EarningsController {

    private final EarningsService earningsService;

    public EarningsController(EarningsService earningsService) {
        this.earningsService = earningsService;
    }

    /**
     * Recupera i dati di earnings per uno o più ticker.
     *
     * @param tickers lista ticker separati da virgola (es. {@code AAPL,MSFT,ISP.MI})
     */
    @GetMapping
    @Operation(summary = "Earnings date e stime EPS per lista ticker")
    public ResponseEntity<List<EarningsDto>> getEarnings(
            @RequestParam(name = "tickers") String tickers) {

        List<String> tickerList = Arrays.stream(tickers.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(t -> !t.isBlank())
                .limit(30)
                .toList();

        return ResponseEntity.ok(earningsService.fetchEarnings(tickerList));
    }
}
