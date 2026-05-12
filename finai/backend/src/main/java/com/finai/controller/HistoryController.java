package com.finai.controller;

import com.finai.dto.quote.HistoryPoint;
import com.finai.service.YahooFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Storico prezzi OHLCV per un ticker. */
@RestController
@RequestMapping("/api/history")
@Tag(name = "History", description = "Storico OHLCV per un titolo")
public class HistoryController {

    private final YahooFinanceService yahoo;

    public HistoryController(YahooFinanceService yahoo) {
        this.yahoo = yahoo;
    }

    /**
     * Storico prezzi OHLCV.
     *
     * @param ticker simbolo Yahoo Finance
     * @param range  "1m" | "3m" | "6m" | "1y" (default "1y")
     */
    @GetMapping("/{ticker}")
    @Operation(summary = "Storico prezzi OHLCV")
    public ResponseEntity<List<HistoryPoint>> history(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1y") String range) {

        return ResponseEntity.ok(yahoo.fetchHistory(ticker.toUpperCase(), range));
    }
}
