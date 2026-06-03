package com.finai.controller;

import com.finai.dto.watchlist.AddWatchlistRequest;
import com.finai.dto.watchlist.WatchlistItemDto;
import com.finai.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD per la watchlist personale.
 */
@RestController
@RequestMapping("/api/watchlist")
@Tag(name = "Watchlist", description = "Watchlist personale ticker")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    /** Restituisce tutti gli elementi della watchlist. */
    @GetMapping
    @Operation(summary = "Lista watchlist")
    public ResponseEntity<List<WatchlistItemDto>> getAll() {
        return ResponseEntity.ok(watchlistService.getAll());
    }

    /**
     * Aggiunge un ticker alla watchlist.
     *
     * @param req dati del ticker da aggiungere
     */
    @PostMapping
    @Operation(summary = "Aggiungi ticker alla watchlist")
    public ResponseEntity<WatchlistItemDto> add(@RequestBody AddWatchlistRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(watchlistService.add(req));
    }

    /**
     * Rimuove un elemento dalla watchlist.
     *
     * @param id ID dell'elemento da rimuovere
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Rimuovi dalla watchlist")
    public ResponseEntity<Void> remove(@PathVariable String id) {
        watchlistService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
