package com.finai.service;

import com.finai.domain.entity.PortfolioItem;
import com.finai.dto.portfolio.AddPortfolioItemRequest;
import com.finai.dto.portfolio.PortfolioItemDto;
import com.finai.dto.quote.QuoteDto;
import com.finai.exception.FinaiException;
import com.finai.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
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
 * Test unitari per {@link PortfolioService}.
 * Usa Mockito per isolare il servizio dal repository e da Yahoo Finance.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService")
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository repository;

    @Mock
    private YahooFinanceService yahooFinanceService;

    @InjectMocks
    private PortfolioService service;

    private PortfolioItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = new PortfolioItem();
        sampleItem.setId("id-1");
        sampleItem.setTicker("AAPL");
        sampleItem.setName("Apple Inc.");
        sampleItem.setQty(new BigDecimal("10"));
        sampleItem.setLoadPrice(new BigDecimal("175.00"));
        sampleItem.setCurrency("USD");
    }

    @Test
    @DisplayName("getAll: delega al repository e mappa in DTO")
    void getAll_delegatesToRepository() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleItem));

        List<PortfolioItemDto> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ticker()).isEqualTo("AAPL");
        assertThat(result.get(0).qty()).isEqualTo(10.0);
        verify(repository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("add: salva la posizione e la restituisce come DTO")
    void add_savesAndReturnsDto() {
        AddPortfolioItemRequest req = new AddPortfolioItemRequest(
                "id-1", "aapl", "Apple Inc.", 10.0, 175.0, "USD");

        when(repository.existsById("id-1")).thenReturn(false);
        when(repository.save(any(PortfolioItem.class))).thenReturn(sampleItem);

        PortfolioItemDto result = service.add(req);

        assertThat(result.id()).isEqualTo("id-1");
        assertThat(result.ticker()).isEqualTo("AAPL"); // uppercase normalizzato dall'entità
        verify(repository).save(argThat(item -> item.getTicker().equals("AAPL")));
    }

    @Test
    @DisplayName("add: lancia FinaiException 409 se ID già presente")
    void add_throwsConflictIfIdExists() {
        AddPortfolioItemRequest req = new AddPortfolioItemRequest(
                "id-1", "AAPL", "Apple", 10.0, 175.0, "USD");
        when(repository.existsById("id-1")).thenReturn(true);

        assertThatThrownBy(() -> service.add(req))
                .isInstanceOf(FinaiException.class)
                .hasMessageContaining("id-1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("remove: elimina per ID")
    void remove_deletesById() {
        when(repository.existsById("id-1")).thenReturn(true);

        service.remove("id-1");

        verify(repository).deleteById("id-1");
    }

    @Test
    @DisplayName("remove: lancia FinaiException 404 se non esiste")
    void remove_throwsNotFoundIfMissing() {
        when(repository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.remove("missing"))
                .isInstanceOf(FinaiException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("refresh: portafoglio vuoto → lista vuota senza chiamate Yahoo")
    void refresh_emptyPortfolio() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<PortfolioItemDto> result = service.refresh();

        assertThat(result).isEmpty();
        verifyNoInteractions(yahooFinanceService);
    }

    @Test
    @DisplayName("refresh: aggiorna i prezzi per ogni ticker trovato")
    void refresh_updatesPricesFromYahoo() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleItem));

        QuoteDto quote = new QuoteDto("AAPL", "Apple Inc.", 190.0, 1.0, 0.5, 12.5,
                200.0, 140.0, 50_000_000L, 3_000_000_000L, 29.0,
                "USD", "NASDAQ", 70, System.currentTimeMillis());
        when(yahooFinanceService.fetchBatch(List.of("AAPL"))).thenReturn(List.of(quote));
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleItem));

        service.refresh();

        verify(yahooFinanceService).fetchBatch(anyList());
        verify(repository, atLeastOnce()).updatePriceByTicker(eq("AAPL"), any(), eq("USD"));
    }
}
