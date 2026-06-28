package com.finai.controller;

import com.finai.dto.simulator.*;
import com.finai.service.SimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ambiente di simulazione investimenti (paper trading): wallet virtuale,
 * acquisti/vendite ai prezzi live di Yahoo Finance, storico operazioni.
 */
@RestController
@RequestMapping("/api/sim")
@Tag(name = "Simulator", description = "Ambiente di simulazione investimenti con moneta virtuale")
public class SimulatorController {

    private final SimulatorService service;

    public SimulatorController(SimulatorService service) {
        this.service = service;
    }

    @GetMapping("/wallet")
    @Operation(summary = "Stato grezzo del wallet virtuale (liquidità e capitale iniziale)")
    public ResponseEntity<SimWalletDto> wallet() {
        return ResponseEntity.ok(SimWalletDto.from(service.getWallet()));
    }

    @GetMapping("/summary")
    @Operation(summary = "Riepilogo simulazione: liquidità, posizioni valorizzate live, P&L totale")
    public ResponseEntity<SimSummaryDto> summary() {
        return ResponseEntity.ok(service.getSummary());
    }

    @GetMapping("/trades")
    @Operation(summary = "Storico operazioni simulate, più recenti prime")
    public ResponseEntity<List<SimTradeDto>> trades() {
        return ResponseEntity.ok(service.getTrades());
    }

    @PostMapping("/buy")
    @Operation(summary = "Acquista un titolo con moneta virtuale al prezzo live")
    public ResponseEntity<SimSummaryDto> buy(@Valid @RequestBody BuyRequest req) {
        return ResponseEntity.ok(service.buy(req));
    }

    @PostMapping("/sell")
    @Operation(summary = "Vende (in parte o totalmente) una posizione simulata al prezzo live")
    public ResponseEntity<SimSummaryDto> sell(@Valid @RequestBody SellRequest req) {
        return ResponseEntity.ok(service.sell(req));
    }

    @PostMapping("/reset")
    @Operation(summary = "Azzera la simulazione e ripristina il capitale iniziale")
    public ResponseEntity<SimSummaryDto> reset(@RequestBody(required = false) ResetRequest req) {
        return ResponseEntity.ok(service.reset(req != null ? req : new ResetRequest(null)));
    }
}
