package com.finai.controller;

import com.finai.dto.screener.ScreenerDto;
import com.finai.service.ScreenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint per lo screener azionario.
 */
@RestController
@RequestMapping("/api/screener")
@Tag(name = "Screener", description = "Screener azionario su universe fisso")
public class ScreenerController {

    private final ScreenerService screenerService;

    public ScreenerController(ScreenerService screenerService) {
        this.screenerService = screenerService;
    }

    /**
     * Esegue lo screening con i filtri specificati.
     *
     * @param minPE    P/E minimo (opzionale)
     * @param maxPE    P/E massimo (opzionale)
     * @param minYield Dividend yield minimo in % (opzionale)
     * @param minYtd   YTD change minimo % (opzionale)
     * @param market   Mercato: "us", "it", "de", "fr", o null per tutti
     * @param limit    Numero max risultati (default 50)
     */
    @GetMapping
    @Operation(summary = "Screener azionario con filtri")
    public ResponseEntity<List<ScreenerDto>> screen(
            @RequestParam(required = false) Double minPE,
            @RequestParam(required = false) Double maxPE,
            @RequestParam(required = false) Double minYield,
            @RequestParam(required = false) Double minYtd,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "50") int limit) {

        int safeLimit = Math.min(Math.max(1, limit), 100);
        return ResponseEntity.ok(screenerService.screen(minPE, maxPE, minYield, minYtd, market, safeLimit));
    }
}
