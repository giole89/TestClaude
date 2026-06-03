package com.finai.service;

import com.finai.dto.correlation.CorrelationDto;
import com.finai.dto.quote.HistoryPoint;
import com.finai.domain.entity.PortfolioItem;
import com.finai.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcola la matrice di correlazione di Pearson sui ritorni giornalieri
 * dei ticker nel portafoglio.
 *
 * <p>Utilizza i dati storici "1y" da {@link YahooFinanceService#fetchHistory}.
 * Limitato a max 8 ticker (i primi 8 per valore di mercato).</p>
 */
@Service
public class CorrelationService {

    private static final Logger log = LoggerFactory.getLogger(CorrelationService.class);
    private static final int MAX_TICKERS = 8;

    private final PortfolioRepository portfolioRepository;
    private final YahooFinanceService yahooFinanceService;

    public CorrelationService(PortfolioRepository portfolioRepository,
                              YahooFinanceService yahooFinanceService) {
        this.portfolioRepository = portfolioRepository;
        this.yahooFinanceService = yahooFinanceService;
    }

    /**
     * Calcola la matrice di correlazione per i ticker del portafoglio.
     *
     * @return CorrelationDto con matrice NxN e interpretazioni
     */
    @Cacheable(value = "correlation", key = "'portfolio'")
    public CorrelationDto calculate() {
        List<PortfolioItem> items = portfolioRepository.findAllByOrderByCreatedAtDesc();
        if (items.isEmpty()) {
            return new CorrelationDto(List.of(), new double[0][0], List.of());
        }

        // Prende i primi MAX_TICKERS per valore di mercato
        List<String> tickers = items.stream()
                .sorted(Comparator.comparingDouble((PortfolioItem i) -> {
                    double price = i.getCurrentPrice() != null
                            ? i.getCurrentPrice().doubleValue()
                            : i.getLoadPrice().doubleValue();
                    return -(i.getQty().doubleValue() * price);
                }))
                .map(PortfolioItem::getTicker)
                .distinct()
                .limit(MAX_TICKERS)
                .collect(Collectors.toList());

        // Raccoglie i ritorni giornalieri per ogni ticker
        Map<String, double[]> returnsMap = new LinkedHashMap<>();
        for (String ticker : tickers) {
            try {
                List<HistoryPoint> history = yahooFinanceService.fetchHistory(ticker, "1y");
                double[] returns = calcDailyReturns(history);
                if (returns.length >= 10) {
                    returnsMap.put(ticker, returns);
                }
            } catch (Exception e) {
                log.debug("Errore storico {} per correlazione: {}", ticker, e.getMessage());
            }
        }

        List<String> labels = new ArrayList<>(returnsMap.keySet());
        int n = labels.size();

        // Caso n=1: matrice 1x1 con diagonale 1.0
        if (n == 1) {
            double[][] single = new double[1][1];
            single[0][0] = 1.0;
            return new CorrelationDto(labels, single, List.of());
        }
        if (n < 1) {
            return new CorrelationDto(labels, new double[0][0], List.of());
        }

        // Allinea le serie temporali alla lunghezza minima comune
        int minLen = returnsMap.values().stream()
                .mapToInt(arr -> arr.length)
                .min()
                .orElse(0);

        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 1.0;
                } else if (j < i) {
                    matrix[i][j] = matrix[j][i]; // simmetrica
                } else {
                    double[] ri = Arrays.copyOf(returnsMap.get(labels.get(i)), minLen);
                    double[] rj = Arrays.copyOf(returnsMap.get(labels.get(j)), minLen);
                    matrix[i][j] = pearson(ri, rj);
                }
            }
        }

        List<String> interpretations = buildInterpretations(labels, matrix);

        return new CorrelationDto(labels, matrix, interpretations);
    }

    /**
     * Calcola i ritorni giornalieri da una serie di prezzi.
     *
     * @param history lista di HistoryPoint
     * @return array di ritorni giornalieri r_t = (p_t - p_{t-1}) / p_{t-1}
     */
    public double[] calcDailyReturns(List<HistoryPoint> history) {
        if (history == null || history.size() < 2) return new double[0];
        double[] returns = new double[history.size() - 1];
        for (int i = 1; i < history.size(); i++) {
            Double prev = history.get(i - 1).close();
            Double curr = history.get(i).close();
            if (prev == null || curr == null || prev == 0) {
                returns[i - 1] = 0.0;
            } else {
                returns[i - 1] = (curr - prev) / prev;
            }
        }
        return returns;
    }

    /**
     * Calcola il coefficiente di correlazione di Pearson tra due array.
     *
     * @param x primo array
     * @param y secondo array (stessa lunghezza di x)
     * @return correlazione in [-1, 1], oppure 0 se non calcolabile
     */
    public double pearson(double[] x, double[] y) {
        if (x.length != y.length || x.length < 2) return 0.0;
        int n = x.length;

        double sumX = 0, sumY = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        double cov = 0, stdX = 0, stdY = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            cov  += dx * dy;
            stdX += dx * dx;
            stdY += dy * dy;
        }

        double denom = Math.sqrt(stdX * stdY);
        if (denom == 0) return 0.0;
        double corr = cov / denom;
        // Clamp to [-1, 1] per errori numerici
        return Math.max(-1.0, Math.min(1.0, corr));
    }

    private List<String> buildInterpretations(List<String> labels, double[][] matrix) {
        List<String> interp = new ArrayList<>();
        int n = labels.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double c = matrix[i][j];
                String level;
                if (c >= 0.7) level = "alta correlazione";
                else if (c >= 0.4) level = "media correlazione";
                else if (c >= 0) level = "bassa correlazione";
                else if (c >= -0.4) level = "bassa correlazione inversa";
                else level = "alta correlazione inversa";
                interp.add(String.format("%s-%s: %s %.2f",
                        labels.get(i), labels.get(j), level, c));
            }
        }
        return interp;
    }
}
