package com.finai.service;

import com.finai.domain.entity.PortfolioItem;
import com.finai.dto.analytics.PortfolioAnalyticsDto;
import com.finai.repository.PortfolioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioAnalyticsService")
class PortfolioAnalyticsServiceTest {

    @Mock
    private PortfolioRepository repository;

    @InjectMocks
    private PortfolioAnalyticsService service;

    @Test
    @DisplayName("portafoglio vuoto → tutti i valori a zero")
    void emptyPortfolio() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        PortfolioAnalyticsDto result = service.compute();

        assertThat(result.totalValue()).isEqualTo(0.0);
        assertThat(result.positionCount()).isEqualTo(0);
        assertThat(result.positions()).isEmpty();
    }

    @Test
    @DisplayName("singola posizione in guadagno → P&L corretto")
    void singlePositionGain() {
        PortfolioItem item = buildItem("AAPL", 10, 150.0, 200.0, "USD");
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item));

        PortfolioAnalyticsDto result = service.compute();

        assertThat(result.totalCost()).isCloseTo(1500.0, within(0.01));
        assertThat(result.totalValue()).isCloseTo(2000.0, within(0.01));
        assertThat(result.totalGainAmount()).isCloseTo(500.0, within(0.01));
        assertThat(result.totalGainPct()).isCloseTo(33.33, within(0.1));
        assertThat(result.bestTicker()).isEqualTo("AAPL");
        assertThat(result.worstTicker()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("due posizioni → best/worst identificati correttamente")
    void bestWorstIdentified() {
        PortfolioItem winner = buildItem("MSFT", 5, 100.0, 150.0, "USD");  // +50%
        PortfolioItem loser  = buildItem("META", 5, 100.0, 80.0,  "USD");  // -20%
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(winner, loser));

        PortfolioAnalyticsDto result = service.compute();

        assertThat(result.bestTicker()).isEqualTo("MSFT");
        assertThat(result.bestGainPct()).isCloseTo(50.0, within(0.1));
        assertThat(result.worstTicker()).isEqualTo("META");
        assertThat(result.worstGainPct()).isCloseTo(-20.0, within(0.1));
    }

    @Test
    @DisplayName("posizione senza currentPrice → usa loadPrice come fallback")
    void missingCurrentPriceUsesLoadPrice() {
        PortfolioItem item = buildItem("ENI.MI", 10, 14.0, null, "EUR");
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item));

        PortfolioAnalyticsDto result = service.compute();

        assertThat(result.totalGainAmount()).isCloseTo(0.0, within(0.01));
        assertThat(result.totalGainPct()).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("HHI portafoglio concentrato su un solo titolo → vicino a 10000")
    void concentratedPortfolioHhi() {
        PortfolioItem single = buildItem("NVDA", 100, 100.0, 500.0, "USD");
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(single));

        PortfolioAnalyticsDto result = service.compute();

        // Con una sola posizione il peso è 100% → HHI = 100*100 = 10000
        assertThat(result.concentrationHhi()).isCloseTo(10000.0, within(1.0));
    }

    @Test
    @DisplayName("posizioni ordinate per peso decrescente")
    void positionsOrderedByWeight() {
        PortfolioItem big   = buildItem("AAPL", 100, 200.0, 200.0, "USD"); // valore 20000
        PortfolioItem small = buildItem("ENI.MI", 10, 15.0, 15.0, "EUR");  // valore 150
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(small, big));

        PortfolioAnalyticsDto result = service.compute();

        assertThat(result.positions().get(0).ticker()).isEqualTo("AAPL");
        assertThat(result.positions().get(1).ticker()).isEqualTo("ENI.MI");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PortfolioItem buildItem(String ticker, int qty,
                                    double loadPrice, Double currentPrice,
                                    String currency) {
        PortfolioItem item = new PortfolioItem();
        item.setId(ticker + "-1");
        item.setTicker(ticker);
        item.setName(ticker + " Inc.");
        item.setQty(BigDecimal.valueOf(qty));
        item.setLoadPrice(BigDecimal.valueOf(loadPrice));
        item.setCurrentPrice(currentPrice != null ? BigDecimal.valueOf(currentPrice) : null);
        item.setCurrency(currency);
        return item;
    }
}
