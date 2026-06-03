package com.finai.controller;

import com.finai.dto.analytics.PortfolioAnalyticsDto;
import com.finai.dto.portfolio.AddPortfolioItemRequest;
import com.finai.dto.portfolio.PortfolioItemDto;
import com.finai.service.PortfolioAnalyticsService;
import com.finai.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CRUD per il portafoglio personale e aggiornamento prezzi live.
 */
@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio", description = "Gestione portafoglio personale")
public class PortfolioController {

    private final PortfolioService          service;
    private final PortfolioAnalyticsService analyticsService;

    public PortfolioController(PortfolioService service,
                               PortfolioAnalyticsService analyticsService) {
        this.service          = service;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Lista tutte le posizioni del portafoglio")
    public ResponseEntity<List<PortfolioItemDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    @Operation(summary = "Aggiunge una posizione al portafoglio")
    public ResponseEntity<PortfolioItemDto> add(@Valid @RequestBody AddPortfolioItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rimuove una posizione dal portafoglio")
    public ResponseEntity<Map<String, Boolean>> remove(@PathVariable String id) {
        service.remove(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * Aggiorna tutti i prezzi correnti tramite Yahoo Finance.
     * Restituisce il portafoglio aggiornato.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Aggiorna i prezzi correnti di tutte le posizioni")
    public ResponseEntity<List<PortfolioItemDto>> refresh() {
        return ResponseEntity.ok(service.refresh());
    }

    /**
     * Calcola metriche aggregate del portafoglio:
     * P&L, best/worst performer, concentrazione HHI, breakdown posizioni.
     */
    @GetMapping("/analytics")
    @Operation(summary = "Analisi aggregata del portafoglio")
    public ResponseEntity<PortfolioAnalyticsDto> analytics() {
        return ResponseEntity.ok(analyticsService.compute());
    }
}
