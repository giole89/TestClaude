package com.finai.service;

import com.finai.dto.correlation.CorrelationDto;
import com.finai.dto.quote.HistoryPoint;
import com.finai.domain.entity.PortfolioItem;
import com.finai.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per {@link CorrelationService} con focus sulla formula Pearson.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationService")
class CorrelationServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private YahooFinanceService yahooFinanceService;

    @InjectMocks
    private CorrelationService service;

    // ── Test formula Pearson ────────────────────────────────────────────────────

    @Test
    @DisplayName("pearson: correlazione perfetta positiva restituisce 1.0")
    void pearson_perfectPositiveCorrelation() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {2, 4, 6, 8, 10};

        double result = service.pearson(x, y);

        assertThat(result).isCloseTo(1.0, within(0.0001));
    }

    @Test
    @DisplayName("pearson: correlazione perfetta negativa restituisce -1.0")
    void pearson_perfectNegativeCorrelation() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {10, 8, 6, 4, 2};

        double result = service.pearson(x, y);

        assertThat(result).isCloseTo(-1.0, within(0.0001));
    }

    @Test
    @DisplayName("pearson: stessa serie con se stessa restituisce 1.0")
    void pearson_selfCorrelation() {
        double[] x = {0.01, -0.02, 0.03, 0.005, -0.01, 0.02};

        double result = service.pearson(x, x);

        assertThat(result).isCloseTo(1.0, within(0.0001));
    }

    @Test
    @DisplayName("pearson: array di lunghezze diverse restituisce 0.0")
    void pearson_differentLengths() {
        double[] x = {1, 2, 3};
        double[] y = {1, 2};

        double result = service.pearson(x, y);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("pearson: array vuoti restituiscono 0.0")
    void pearson_emptyArrays() {
        double[] x = {};
        double[] y = {};

        double result = service.pearson(x, y);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("pearson: varianza zero restituisce 0.0 (no divisione per zero)")
    void pearson_zeroVariance() {
        double[] x = {1, 1, 1, 1};
        double[] y = {1, 2, 3, 4};

        double result = service.pearson(x, y);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("pearson: il risultato è sempre in [-1, 1]")
    void pearson_resultInValidRange() {
        double[] x = {0.01, -0.005, 0.02, -0.01, 0.008, 0.015, -0.003};
        double[] y = {0.005, -0.002, 0.015, -0.008, 0.006, 0.012, -0.001};

        double result = service.pearson(x, y);

        assertThat(result).isBetween(-1.0, 1.0);
    }

    // ── Test calcDailyReturns ───────────────────────────────────────────────────

    @Test
    @DisplayName("calcDailyReturns: calcola correttamente i ritorni giornalieri")
    void calcDailyReturns_correctCalculation() {
        List<HistoryPoint> history = List.of(
                new HistoryPoint("2025-01-01", 100.0, 105.0, 99.0, 100.0, 1000L),
                new HistoryPoint("2025-01-02", 100.0, 112.0, 99.0, 110.0, 1000L),  // +10%
                new HistoryPoint("2025-01-03", 110.0, 115.0, 105.0, 99.0, 1000L)   // -10%
        );

        double[] returns = service.calcDailyReturns(history);

        assertThat(returns).hasSize(2);
        assertThat(returns[0]).isCloseTo(0.10, within(0.0001)); // (110-100)/100
        assertThat(returns[1]).isCloseTo(-0.10, within(0.001)); // (99-110)/110 ≈ -0.1
    }

    @Test
    @DisplayName("calcDailyReturns: serie con meno di 2 elementi restituisce array vuoto")
    void calcDailyReturns_tooShortHistory() {
        List<HistoryPoint> singlePoint = List.of(
                new HistoryPoint("2025-01-01", 100.0, 105.0, 99.0, 100.0, 1000L)
        );

        double[] returns = service.calcDailyReturns(singlePoint);

        assertThat(returns).isEmpty();
    }

    @Test
    @DisplayName("calcDailyReturns: serie null restituisce array vuoto")
    void calcDailyReturns_nullHistory() {
        double[] returns = service.calcDailyReturns(null);

        assertThat(returns).isEmpty();
    }

    // ── Test calculate ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("calculate: portafoglio vuoto restituisce DTO con labels vuoti")
    void calculate_emptyPortfolio() {
        when(portfolioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        CorrelationDto result = service.calculate();

        assertThat(result.labels()).isEmpty();
        assertThat(result.matrix()).isEmpty();
        assertThat(result.interpretations()).isEmpty();
    }

    @Test
    @DisplayName("calculate: portafoglio con un solo ticker restituisce matrice 1x1")
    void calculate_singleTicker() {
        PortfolioItem item = new PortfolioItem();
        item.setTicker("AAPL");
        item.setQty(BigDecimal.TEN);
        item.setLoadPrice(BigDecimal.valueOf(150));
        item.setCurrentPrice(BigDecimal.valueOf(180));

        when(portfolioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item));

        // Mock storico con abbastanza punti
        List<HistoryPoint> history = generateHistory(100, 100.0);
        when(yahooFinanceService.fetchHistory(eq("AAPL"), anyString())).thenReturn(history);

        CorrelationDto result = service.calculate();

        assertThat(result.labels()).containsExactly("AAPL");
        assertThat(result.matrix().length).isEqualTo(1);
        assertThat(result.matrix()[0][0]).isCloseTo(1.0, within(0.0001));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<HistoryPoint> generateHistory(int days, double startPrice) {
        HistoryPoint[] points = new HistoryPoint[days];
        double price = startPrice;
        for (int i = 0; i < days; i++) {
            price = price * (1 + (Math.random() - 0.5) * 0.02);
            points[i] = new HistoryPoint("2025-01-" + String.format("%02d", (i % 28) + 1),
                    price, price * 1.01, price * 0.99, price, 1000L);
        }
        return Arrays.asList(points);
    }
}
