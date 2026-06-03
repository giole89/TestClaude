package com.finai.service;

import com.finai.dto.benchmark.BenchmarkDto;
import com.finai.dto.quote.HistoryPoint;
import com.finai.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Confronta la performance del portafoglio vs S&P 500 (^GSPC).
 *
 * <p>Usa {@link YahooFinanceService#fetchHistory} già esistente per recuperare
 * i dati storici di ogni ticker nel portafoglio e del benchmark S&P 500.</p>
 *
 * <p>Il rendimento del portafoglio è calcolato come media pesata (per valore di mercato)
 * dei rendimenti individuali dei titoli nel periodo.</p>
 */
@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);
    private static final String SP500 = "^GSPC";

    private final PortfolioRepository portfolioRepository;
    private final YahooFinanceService yahooFinanceService;

    public BenchmarkService(PortfolioRepository portfolioRepository,
                            YahooFinanceService yahooFinanceService) {
        this.portfolioRepository  = portfolioRepository;
        this.yahooFinanceService  = yahooFinanceService;
    }

    /**
     * Calcola benchmark portafoglio vs S&P 500 per il periodo specificato.
     *
     * @param period "3m", "6m", "1y", "3y"
     * @return BenchmarkDto con rendimenti e alpha
     */
    @Cacheable(value = "benchmark", key = "#period")
    public BenchmarkDto calculate(String period) {
        // Recupera portafoglio
        var items = portfolioRepository.findAllByOrderByCreatedAtDesc();
        if (items.isEmpty()) {
            return new BenchmarkDto(0.0, 0.0, 0.0, period, "Portafoglio", "S&P 500");
        }

        // Calcola valore totale portafoglio (per pesi)
        double totalValue = items.stream()
                .mapToDouble(i -> {
                    double price = i.getCurrentPrice() != null
                            ? i.getCurrentPrice().doubleValue()
                            : i.getLoadPrice().doubleValue();
                    return i.getQty().doubleValue() * price;
                })
                .sum();

        if (totalValue <= 0) {
            return new BenchmarkDto(0.0, 0.0, 0.0, period, "Portafoglio", "S&P 500");
        }

        // Per ogni ticker, calcola rendimento nel periodo e peso
        double weightedReturn = 0.0;
        double totalWeight    = 0.0;

        for (var item : items) {
            try {
                List<HistoryPoint> history = yahooFinanceService.fetchHistory(item.getTicker(), period);
                double ret = calcReturn(history);
                if (Double.isNaN(ret) || Double.isInfinite(ret)) continue;

                double itemValue = item.getQty().doubleValue() *
                        (item.getCurrentPrice() != null
                                ? item.getCurrentPrice().doubleValue()
                                : item.getLoadPrice().doubleValue());
                double weight = itemValue / totalValue;

                weightedReturn += ret * weight;
                totalWeight    += weight;
            } catch (Exception e) {
                log.debug("Errore storico per {}: {}", item.getTicker(), e.getMessage());
            }
        }

        double portfolioReturn = totalWeight > 0 ? weightedReturn / totalWeight * 100 : 0;

        // Rendimento S&P 500
        double benchmarkReturn = 0.0;
        try {
            List<HistoryPoint> sp500History = yahooFinanceService.fetchHistory(SP500, period);
            benchmarkReturn = calcReturn(sp500History) * 100;
        } catch (Exception e) {
            log.warn("Errore storico S&P 500: {}", e.getMessage());
        }

        double alpha = portfolioReturn - benchmarkReturn;

        return new BenchmarkDto(
                round(portfolioReturn),
                round(benchmarkReturn),
                round(alpha),
                period,
                "Portafoglio",
                "S&P 500"
        );
    }

    /**
     * Calcola il rendimento percentuale su una serie storica.
     * Rendimento = (ultimo - primo) / primo
     *
     * @return rendimento come decimale (es. 0.12 = 12%)
     */
    public double calcReturn(List<HistoryPoint> history) {
        if (history == null || history.size() < 2) return 0.0;
        double first = history.get(0).close();
        double last  = history.get(history.size() - 1).close();
        if (first == 0) return 0.0;
        return (last - first) / first;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
