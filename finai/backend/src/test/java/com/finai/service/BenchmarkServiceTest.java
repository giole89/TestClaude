package com.finai.service;

import com.finai.domain.entity.PortfolioItem;
import com.finai.dto.benchmark.BenchmarkDto;
import com.finai.dto.quote.HistoryPoint;
import com.finai.repository.PortfolioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per {@link BenchmarkService}.
 * Testa il calcolo del rendimento e dell'alpha.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BenchmarkService")
class BenchmarkServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private YahooFinanceService yahooFinanceService;

    @InjectMocks
    private BenchmarkService service;

    // ── Test calcReturn ────────────────────────────────────────────────────────

    @Test
    @DisplayName("calcReturn: calcola il rendimento correttamente")
    void calcReturn_basicCalculation() {
        List<HistoryPoint> history = List.of(
                new HistoryPoint("2025-01-01", 100.0, 105.0, 99.0, 100.0, 1000L),
                new HistoryPoint("2025-12-31", 120.0, 125.0, 119.0, 120.0, 1000L)
        );

        double result = service.calcReturn(history);

        assertThat(result).isCloseTo(0.20, within(0.0001)); // (120-100)/100 = 20%
    }

    @Test
    @DisplayName("calcReturn: lista vuota restituisce 0.0")
    void calcReturn_emptyList() {
        double result = service.calcReturn(List.of());

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calcReturn: lista con un solo punto restituisce 0.0")
    void calcReturn_singlePoint() {
        List<HistoryPoint> history = List.of(
                new HistoryPoint("2025-01-01", 100.0, 105.0, 99.0, 100.0, 1000L)
        );

        double result = service.calcReturn(history);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calcReturn: rendimento negativo calcolato correttamente")
    void calcReturn_negativeReturn() {
        List<HistoryPoint> history = List.of(
                new HistoryPoint("2025-01-01", 100.0, 105.0, 99.0, 100.0, 1000L),
                new HistoryPoint("2025-12-31", 80.0, 85.0, 79.0, 80.0, 1000L)
        );

        double result = service.calcReturn(history);

        assertThat(result).isCloseTo(-0.20, within(0.0001)); // (80-100)/100 = -20%
    }

    // ── Test calculate ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("calculate: portafoglio vuoto restituisce zero per tutti i valori")
    void calculate_emptyPortfolio() {
        when(portfolioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        BenchmarkDto result = service.calculate("1y");

        assertThat(result.portfolioReturn()).isEqualTo(0.0);
        assertThat(result.benchmarkReturn()).isEqualTo(0.0);
        assertThat(result.alpha()).isEqualTo(0.0);
        assertThat(result.period()).isEqualTo("1y");
    }

    @Test
    @DisplayName("calculate: alpha positivo quando portafoglio batte benchmark")
    void calculate_positiveAlpha() {
        // Portfolio: AAPL +30%
        PortfolioItem item = new PortfolioItem();
        item.setTicker("AAPL");
        item.setQty(BigDecimal.TEN);
        item.setLoadPrice(BigDecimal.valueOf(150));
        item.setCurrentPrice(BigDecimal.valueOf(195));

        when(portfolioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item));

        // AAPL +30%
        List<HistoryPoint> aaplHistory = List.of(
                new HistoryPoint("2025-01-01", 100.0, 105.0, 99.0, 100.0, 1000L),
                new HistoryPoint("2025-12-31", 130.0, 135.0, 129.0, 130.0, 1000L)
        );
        when(yahooFinanceService.fetchHistory(eq("AAPL"), anyString())).thenReturn(aaplHistory);

        // S&P 500 +10%
        List<HistoryPoint> sp500History = List.of(
                new HistoryPoint("2025-01-01", 4500.0, 4520.0, 4480.0, 4500.0, 1000000L),
                new HistoryPoint("2025-12-31", 4950.0, 4970.0, 4930.0, 4950.0, 1000000L)
        );
        when(yahooFinanceService.fetchHistory(eq("^GSPC"), anyString())).thenReturn(sp500History);

        BenchmarkDto result = service.calculate("1y");

        assertThat(result.alpha()).isPositive();
        assertThat(result.portfolioReturn()).isGreaterThan(result.benchmarkReturn());
    }

    @Test
    @DisplayName("calculate: alpha negativo quando portafoglio sottoperforma benchmark")
    void calculate_negativeAlpha() {
        PortfolioItem item = new PortfolioItem();
        item.setTicker("WEAK");
        item.setQty(BigDecimal.TEN);
        item.setLoadPrice(BigDecimal.valueOf(100));
        item.setCurrentPrice(BigDecimal.valueOf(95));

        when(portfolioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item));

        // WEAK -5%
        List<HistoryPoint> weakHistory = List.of(
                new HistoryPoint("2025-01-01", 100.0, 105.0, 99.0, 100.0, 1000L),
                new HistoryPoint("2025-12-31", 95.0, 97.0, 94.0, 95.0, 1000L)
        );
        when(yahooFinanceService.fetchHistory(eq("WEAK"), anyString())).thenReturn(weakHistory);

        // S&P 500 +15%
        List<HistoryPoint> sp500History = List.of(
                new HistoryPoint("2025-01-01", 4500.0, 4520.0, 4480.0, 4500.0, 1000000L),
                new HistoryPoint("2025-12-31", 5175.0, 5190.0, 5160.0, 5175.0, 1000000L)
        );
        when(yahooFinanceService.fetchHistory(eq("^GSPC"), anyString())).thenReturn(sp500History);

        BenchmarkDto result = service.calculate("1y");

        assertThat(result.alpha()).isNegative();
        assertThat(result.portfolioReturn()).isLessThan(result.benchmarkReturn());
    }

    @Test
    @DisplayName("calculate: alpha è esattamente portfolioReturn - benchmarkReturn")
    void calculate_alphaConsistency() {
        PortfolioItem item = new PortfolioItem();
        item.setTicker("TEST");
        item.setQty(BigDecimal.TEN);
        item.setLoadPrice(BigDecimal.valueOf(100));
        item.setCurrentPrice(BigDecimal.valueOf(120));

        when(portfolioRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item));

        List<HistoryPoint> history = List.of(
                new HistoryPoint("2025-01-01", 100.0, 105.0, 99.0, 100.0, 1000L),
                new HistoryPoint("2025-12-31", 120.0, 125.0, 119.0, 120.0, 1000L)
        );
        when(yahooFinanceService.fetchHistory(eq("TEST"), anyString())).thenReturn(history);

        List<HistoryPoint> sp500History = List.of(
                new HistoryPoint("2025-01-01", 4500.0, 4520.0, 4480.0, 4500.0, 1000000L),
                new HistoryPoint("2025-12-31", 4815.0, 4830.0, 4800.0, 4815.0, 1000000L)
        );
        when(yahooFinanceService.fetchHistory(eq("^GSPC"), anyString())).thenReturn(sp500History);

        BenchmarkDto result = service.calculate("1y");

        double expectedAlpha = Math.round((result.portfolioReturn() - result.benchmarkReturn()) * 100.0) / 100.0;
        assertThat(result.alpha()).isCloseTo(expectedAlpha, within(0.01));
    }
}
