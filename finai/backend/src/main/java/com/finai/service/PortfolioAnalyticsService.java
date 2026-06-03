package com.finai.service;

import com.finai.domain.entity.PortfolioItem;
import com.finai.dto.analytics.PortfolioAnalyticsDto;
import com.finai.dto.analytics.PositionStats;
import com.finai.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcola metriche aggregate del portafoglio a partire dai dati persistiti.
 *
 * <p>Tutte le metriche si basano sui prezzi già presenti in DB
 * (aggiornati da {@link PortfolioService#refresh()}), senza effettuare
 * ulteriori chiamate a Yahoo Finance per mantenere la risposta veloce.</p>
 *
 * <p>Metriche calcolate:
 * <ul>
 *   <li>Valore totale / costo totale / P&L</li>
 *   <li>Best / worst performer</li>
 *   <li>Concentrazione HHI (Herfindahl–Hirschman Index)</li>
 *   <li>Breakdown per posizione con peso e contribuzione</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PortfolioAnalyticsService {

    private final PortfolioRepository repository;

    public PortfolioAnalyticsService(PortfolioRepository repository) {
        this.repository = repository;
    }

    public PortfolioAnalyticsDto compute() {
        List<PortfolioItem> items = repository.findAllByOrderByCreatedAtDesc();

        if (items.isEmpty()) {
            return new PortfolioAnalyticsDto(
                    0.0, 0.0, 0.0, 0.0,
                    0, null, null, null, null, null, null,
                    0.0, 0, List.of()
            );
        }

        // ── Totali ────────────────────────────────────────────────────────────
        double totalValue = items.stream()
                .mapToDouble(i -> i.getQty().doubleValue()
                        * effectivePrice(i))
                .sum();

        double totalCost = items.stream()
                .mapToDouble(i -> i.getQty().doubleValue()
                        * i.getLoadPrice().doubleValue())
                .sum();

        double totalGain    = totalValue - totalCost;
        double totalGainPct = totalCost > 0 ? (totalGain / totalCost) * 100.0 : 0.0;

        // ── Posizioni ─────────────────────────────────────────────────────────
        List<PositionStats> positions = items.stream()
                .map(i -> buildPositionStats(i, totalValue, totalCost))
                .sorted(Comparator.comparingDouble(PositionStats::weight).reversed())
                .toList();

        // ── Best / worst ──────────────────────────────────────────────────────
        PositionStats best  = positions.stream()
                .max(Comparator.comparingDouble(PositionStats::gainPct)).orElse(null);
        PositionStats worst = positions.stream()
                .min(Comparator.comparingDouble(PositionStats::gainPct)).orElse(null);

        // ── Concentrazione HHI ────────────────────────────────────────────────
        // HHI = somma dei quadrati dei pesi (in %). <1500=diversificato, >2500=concentrato
        double hhi = positions.stream()
                .mapToDouble(p -> p.weight() * p.weight())
                .sum();

        // ── Top peso ──────────────────────────────────────────────────────────
        PositionStats topWeight = positions.isEmpty() ? null : positions.get(0);

        // ── Divise uniche ─────────────────────────────────────────────────────
        Set<String> currencies = items.stream()
                .map(i -> i.getCurrency() != null ? i.getCurrency() : "USD")
                .collect(Collectors.toSet());

        return new PortfolioAnalyticsDto(
                round(totalValue),
                round(totalCost),
                round(totalGain),
                round(totalGainPct),
                items.size(),
                best  != null ? best.ticker()  : null,
                best  != null ? round(best.gainPct())  : null,
                worst != null ? worst.ticker() : null,
                worst != null ? round(worst.gainPct()) : null,
                topWeight != null ? topWeight.ticker()    : null,
                topWeight != null ? round(topWeight.weight()) : null,
                round(hhi),
                currencies.size(),
                positions
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PositionStats buildPositionStats(PortfolioItem item,
                                              double totalValue,
                                              double totalCost) {
        double price   = effectivePrice(item);
        double load    = item.getLoadPrice().doubleValue();
        double qty     = item.getQty().doubleValue();
        double value   = qty * price;
        double cost    = qty * load;
        double gain    = value - cost;
        double gainPct = cost > 0 ? (gain / cost) * 100.0 : 0.0;
        double weight  = totalValue > 0 ? (value / totalValue) * 100.0 : 0.0;
        // contribuzione = quanto questo titolo ha mosso il portafoglio totale
        double contrib = totalCost > 0 ? (gain / totalCost) * 100.0 : 0.0;

        return new PositionStats(
                item.getTicker(),
                item.getName(),
                qty,
                round(load),
                round(price),
                round(value),
                round(cost),
                round(gain),
                round(gainPct),
                round(weight),
                round(contrib),
                item.getCurrency() != null ? item.getCurrency() : "USD"
        );
    }

    /** Usa currentPrice se disponibile, altrimenti loadPrice. */
    private double effectivePrice(PortfolioItem item) {
        return item.getCurrentPrice() != null
                ? item.getCurrentPrice().doubleValue()
                : item.getLoadPrice().doubleValue();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
