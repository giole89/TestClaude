package com.finai.controller;

import com.finai.dto.quote.QuoteDto;
import com.finai.service.YahooFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Quote di più ticker in una singola chiamata.
 * Usato dalla dashboard mercato per popolare top/worst grid.
 */
@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch", description = "Quote multiple in una sola chiamata")
public class BatchController {

    private final YahooFinanceService yahoo;

    public BatchController(YahooFinanceService yahoo) {
        this.yahoo = yahoo;
    }

    /**
     * Recupera le quote di un elenco di ticker separati da virgola.
     *
     * @param tickers es. {@code AAPL,MSFT,ISP.MI}
     */
    @GetMapping
    @Operation(summary = "Quote batch di più ticker")
    public ResponseEntity<List<QuoteDto>> batch(
            @RequestParam(name = "tickers") String tickers) {

        List<String> tickerList = Arrays.stream(tickers.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(t -> !t.isBlank())
                .limit(50) // max 50 ticker per chiamata
                .toList();

        return ResponseEntity.ok(yahoo.fetchBatch(tickerList));
    }
}
