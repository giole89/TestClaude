package com.finai.controller;

import com.finai.dto.alert.AddAlertRequest;
import com.finai.dto.alert.AlertDto;
import com.finai.dto.alert.AlertsResponse;
import com.finai.dto.alert.FireAlertRequest;
import com.finai.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** CRUD per gli alert sui prezzi. */
@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Alert sui prezzi dei titoli")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista alert attivi e history")
    public ResponseEntity<AlertsResponse> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    @Operation(summary = "Crea un nuovo alert")
    public ResponseEntity<AlertDto> add(@Valid @RequestBody AddAlertRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un alert")
    public ResponseEntity<Map<String, Boolean>> remove(@PathVariable String id) {
        service.remove(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * Segna un alert come scattato al prezzo specificato.
     * L'alert passa da "active" a "history".
     */
    @PostMapping("/{id}/fire")
    @Operation(summary = "Segna un alert come scattato")
    public ResponseEntity<Map<String, Boolean>> fire(
            @PathVariable String id,
            @Valid @RequestBody FireAlertRequest req) {
        service.fire(id, req.price());
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
