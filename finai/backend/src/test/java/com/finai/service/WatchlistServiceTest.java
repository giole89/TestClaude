package com.finai.service;

import com.finai.domain.entity.WatchlistItem;
import com.finai.dto.watchlist.AddWatchlistRequest;
import com.finai.dto.watchlist.WatchlistItemDto;
import com.finai.exception.FinaiException;
import com.finai.repository.WatchlistRepository;
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
 * Test unitari per {@link WatchlistService}.
 * Testa add/remove/duplicati.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WatchlistService")
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository repository;

    @InjectMocks
    private WatchlistService service;

    // ── Test add ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add: aggiunge un ticker con successo")
    void add_success() {
        String ticker = "AAPL";
        AddWatchlistRequest req = new AddWatchlistRequest(
                "id-123", ticker, "Apple Inc.", 200.0, "test"
        );

        when(repository.existsByTickerIgnoreCase(ticker)).thenReturn(false);
        WatchlistItem savedItem = new WatchlistItem();
        savedItem.setId("id-123");
        savedItem.setTicker("AAPL");
        savedItem.setName("Apple Inc.");
        savedItem.setTargetPrice(BigDecimal.valueOf(200.0));
        savedItem.setNote("test");
        savedItem.setCreatedAt(System.currentTimeMillis());

        when(repository.save(any(WatchlistItem.class))).thenReturn(savedItem);

        WatchlistItemDto result = service.add(req);

        assertThat(result.ticker()).isEqualTo("AAPL");
        assertThat(result.name()).isEqualTo("Apple Inc.");
        assertThat(result.targetPrice()).isEqualTo(200.0);
        verify(repository).save(any(WatchlistItem.class));
    }

    @Test
    @DisplayName("add: ticker viene normalizzato in uppercase")
    void add_tickerUppercase() {
        AddWatchlistRequest req = new AddWatchlistRequest(
                "id-123", "aapl", "Apple Inc.", null, null
        );

        when(repository.existsByTickerIgnoreCase("AAPL")).thenReturn(false);
        WatchlistItem savedItem = new WatchlistItem();
        savedItem.setId("id-123");
        savedItem.setTicker("AAPL");
        savedItem.setName("Apple Inc.");
        savedItem.setCreatedAt(System.currentTimeMillis());

        when(repository.save(any(WatchlistItem.class))).thenReturn(savedItem);

        service.add(req);

        verify(repository).existsByTickerIgnoreCase("AAPL");
    }

    @Test
    @DisplayName("add: duplicato lancia FinaiException con status 409")
    void add_duplicate_throws409() {
        AddWatchlistRequest req = new AddWatchlistRequest(
                "id-123", "AAPL", "Apple Inc.", null, null
        );

        when(repository.existsByTickerIgnoreCase("AAPL")).thenReturn(true);

        assertThatThrownBy(() -> service.add(req))
                .isInstanceOf(FinaiException.class)
                .hasMessageContaining("AAPL")
                .satisfies(e -> assertThat(((FinaiException) e).getStatusCode()).isEqualTo(409));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("add: ticker vuoto lancia FinaiException con status 400")
    void add_emptyTicker_throws400() {
        AddWatchlistRequest req = new AddWatchlistRequest(
                "id-123", "  ", null, null, null
        );

        assertThatThrownBy(() -> service.add(req))
                .isInstanceOf(FinaiException.class)
                .satisfies(e -> assertThat(((FinaiException) e).getStatusCode()).isEqualTo(400));

        verify(repository, never()).save(any());
    }

    // ── Test remove ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("remove: rimuove un elemento esistente con successo")
    void remove_success() {
        when(repository.existsById("id-123")).thenReturn(true);

        service.remove("id-123");

        verify(repository).deleteById("id-123");
    }

    @Test
    @DisplayName("remove: elemento non trovato lancia FinaiException con status 404")
    void remove_notFound_throws404() {
        when(repository.existsById("non-existent")).thenReturn(false);

        assertThatThrownBy(() -> service.remove("non-existent"))
                .isInstanceOf(FinaiException.class)
                .satisfies(e -> assertThat(((FinaiException) e).getStatusCode()).isEqualTo(404));

        verify(repository, never()).deleteById(any());
    }

    // ── Test getAll ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: restituisce lista vuota se non ci sono elementi")
    void getAll_empty() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<WatchlistItemDto> result = service.getAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAll: restituisce lista con elementi")
    void getAll_withItems() {
        WatchlistItem item1 = new WatchlistItem();
        item1.setId("id-1");
        item1.setTicker("AAPL");
        item1.setName("Apple");
        item1.setCreatedAt(1000L);

        WatchlistItem item2 = new WatchlistItem();
        item2.setId("id-2");
        item2.setTicker("MSFT");
        item2.setName("Microsoft");
        item2.setCreatedAt(2000L);

        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item2, item1));

        List<WatchlistItemDto> result = service.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).ticker()).isEqualTo("MSFT");
        assertThat(result.get(1).ticker()).isEqualTo("AAPL");
    }
}
