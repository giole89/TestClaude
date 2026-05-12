package com.finai.controller;

import com.finai.dto.search.SearchResultDto;
import com.finai.exception.FinaiException;
import com.finai.service.YahooFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Autocomplete ticker e nomi strumenti via Yahoo Finance search. */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Ricerca autocomplete ticker e nomi")
public class SearchController {

    private final YahooFinanceService yahoo;

    public SearchController(YahooFinanceService yahoo) {
        this.yahoo = yahoo;
    }

    /**
     * Ricerca ticker per nome o simbolo.
     *
     * @param q query di ricerca (minimo 2 caratteri)
     * @return lista di risultati (max 10)
     */
    @GetMapping
    @Operation(summary = "Ricerca autocomplete ticker")
    public ResponseEntity<List<SearchResultDto>> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            throw new FinaiException("Query troppo corta: minimo 2 caratteri", 400);
        }
        return ResponseEntity.ok(yahoo.search(q.trim().toUpperCase()));
    }
}
