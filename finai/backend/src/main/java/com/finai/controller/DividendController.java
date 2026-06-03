package com.finai.controller;

import com.finai.dto.dividend.DividendDto;
import com.finai.service.DividendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Endpoint per i dati dividendi dei titoli.
 */
@RestController
@RequestMapping("/api/dividends")
@Tag(name = "Dividendi", description = "Dati dividendi per ticker")
public class DividendController {

    private final DividendService dividendService;

    public DividendController(DividendService dividendService) {
        this.dividendService = dividendService;
    }

    /**
     * Recupera i dati dividendi per uno o più ticker.
     * Restituisce solo i ticker con dividendo positivo.
     *
     * @param tickers lista ticker separati da virgola (es. {@code AAPL,MSFT,ISP.MI})
     */
    @GetMapping
    @Operation(summary = "Dati dividendi per lista ticker")
    public ResponseEntity<List<DividendDto>> getDividends(
            @RequestParam(name = "tickers") String tickers) {

        List<String> tickerList = Arrays.stream(tickers.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(t -> !t.isBlank())
                .limit(30)
                .toList();

        return ResponseEntity.ok(dividendService.fetchDividends(tickerList));
    }
}
