package com.finai.controller;

import com.finai.dto.ipo.*;
import com.finai.service.IpoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Calendario IPO e watchlist personale. */
@RestController
@RequestMapping("/api/ipo")
@Tag(name = "IPO", description = "Calendario IPO e watchlist personale")
public class IpoController {

    private final IpoService service;

    public IpoController(IpoService service) {
        this.service = service;
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Prossime IPO dal calendario NASDAQ")
    public ResponseEntity<List<UpcomingIpoDto>> upcoming() {
        return ResponseEntity.ok(service.getUpcoming());
    }

    @GetMapping("/recent")
    @Operation(summary = "IPO recenti con performance rispetto al prezzo IPO")
    public ResponseEntity<List<RecentIpoDto>> recent() {
        return ResponseEntity.ok(service.getRecent());
    }

    // ─── Watchlist ────────────────────────────────────────────────────────────

    @GetMapping("/watchlist")
    @Operation(summary = "Watchlist IPO personale")
    public ResponseEntity<List<IpoWatchlistItemDto>> getWatchlist() {
        return ResponseEntity.ok(service.getWatchlist());
    }

    @PostMapping("/watchlist")
    @Operation(summary = "Aggiunge un IPO alla watchlist")
    public ResponseEntity<IpoWatchlistItemDto> addToWatchlist(
            @Valid @RequestBody AddIpoWatchlistRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addToWatchlist(req));
    }

    @PatchMapping("/watchlist/{id}")
    @Operation(summary = "Aggiornamento parziale di un elemento watchlist")
    public ResponseEntity<IpoWatchlistItemDto> updateWatchlist(
            @PathVariable String id,
            @RequestBody UpdateIpoWatchlistRequest req) {
        return ResponseEntity.ok(service.updateWatchlist(id, req));
    }

    @DeleteMapping("/watchlist/{id}")
    @Operation(summary = "Rimuove un elemento dalla watchlist")
    public ResponseEntity<Map<String, Boolean>> removeFromWatchlist(@PathVariable String id) {
        service.removeFromWatchlist(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
