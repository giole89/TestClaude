package com.finai.controller;

import com.finai.dto.benchmark.BenchmarkDto;
import com.finai.service.BenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint per il confronto portafoglio vs benchmark.
 */
@RestController
@RequestMapping("/api/portfolio/benchmark")
@Tag(name = "Benchmark", description = "Performance portafoglio vs S&P 500")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    /**
     * Calcola la performance del portafoglio vs S&P 500 nel periodo specificato.
     *
     * @param period periodo: "3m", "6m", "1y" (default "1y")
     */
    @GetMapping
    @Operation(summary = "Performance portafoglio vs S&P 500")
    public ResponseEntity<BenchmarkDto> getBenchmark(
            @RequestParam(name = "period", defaultValue = "1y") String period) {

        // Valida il periodo
        String validPeriod = switch (period.toLowerCase()) {
            case "3m", "6m", "1y", "3y" -> period.toLowerCase();
            default -> "1y";
        };

        return ResponseEntity.ok(benchmarkService.calculate(validPeriod));
    }
}
