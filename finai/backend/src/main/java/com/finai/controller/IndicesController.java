package com.finai.controller;

import com.finai.dto.quote.QuoteDto;
import com.finai.service.YahooFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Quote degli indici globali di riferimento mostrati nella IndexBar.
 */
@RestController
@RequestMapping("/api/indices")
@Tag(name = "Indices", description = "Indici di mercato globali")
public class IndicesController {

    /** Indici mostrati nella IndexBar: borsa, VIX, forex, materie prime. */
    private static final List<String> INDEX_TICKERS = List.of(
            "^GSPC",      // S&P 500
            "^NDX",       // Nasdaq 100
            "^DJI",       // Dow Jones
            "^STOXX50E",  // Euro Stoxx 50
            "FTSEMIB.MI", // FTSE MIB
            "^VIX",       // VIX
            "EURUSD=X",   // EUR/USD
            "GC=F",       // Oro
            "CL=F"        // WTI Oil
    );

    private final YahooFinanceService yahoo;

    public IndicesController(YahooFinanceService yahoo) {
        this.yahoo = yahoo;
    }

    /** Restituisce le quote di tutti i 9 indici in un singolo batch. */
    @GetMapping
    @Operation(summary = "Quote degli indici globali di riferimento")
    public ResponseEntity<List<QuoteDto>> indices() {
        return ResponseEntity.ok(yahoo.fetchBatch(INDEX_TICKERS));
    }
}
