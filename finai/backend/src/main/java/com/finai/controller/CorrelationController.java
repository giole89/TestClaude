package com.finai.controller;

import com.finai.dto.correlation.CorrelationDto;
import com.finai.service.CorrelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint per la matrice di correlazione del portafoglio.
 */
@RestController
@RequestMapping("/api/portfolio/correlation")
@Tag(name = "Correlazione", description = "Matrice correlazione Pearson portafoglio")
public class CorrelationController {

    private final CorrelationService correlationService;

    public CorrelationController(CorrelationService correlationService) {
        this.correlationService = correlationService;
    }

    /**
     * Calcola la matrice di correlazione di Pearson per i ticker del portafoglio.
     * Usa dati storici "1y" e limita a max 8 ticker per valore di mercato.
     */
    @GetMapping
    @Operation(summary = "Matrice correlazione portafoglio")
    public ResponseEntity<CorrelationDto> getCorrelation() {
        return ResponseEntity.ok(correlationService.calculate());
    }
}
