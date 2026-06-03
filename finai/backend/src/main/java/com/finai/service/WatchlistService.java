package com.finai.service;

import com.finai.domain.entity.WatchlistItem;
import com.finai.dto.watchlist.AddWatchlistRequest;
import com.finai.dto.watchlist.WatchlistItemDto;
import com.finai.exception.FinaiException;
import com.finai.repository.WatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business logic per la gestione della watchlist personale.
 *
 * <p>Ogni ticker può essere aggiunto una sola volta (UNIQUE constraint).
 * Il prezzo target è opzionale.</p>
 */
@Service
@Transactional(readOnly = true)
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    private final WatchlistRepository repository;

    public WatchlistService(WatchlistRepository repository) {
        this.repository = repository;
    }

    /** Restituisce tutti gli elementi ordinati per data di aggiunta discendente. */
    public List<WatchlistItemDto> getAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(WatchlistItemDto::from)
                .toList();
    }

    /**
     * Aggiunge un ticker alla watchlist.
     *
     * @throws FinaiException se il ticker è già presente (409)
     */
    @Transactional
    public WatchlistItemDto add(AddWatchlistRequest req) {
        if (req.ticker() == null || req.ticker().isBlank()) {
            throw new FinaiException("Ticker non valido", 400);
        }
        String ticker = req.ticker().trim().toUpperCase();

        if (repository.existsByTickerIgnoreCase(ticker)) {
            throw new FinaiException("Ticker " + ticker + " già nella watchlist", 409);
        }

        WatchlistItem item = new WatchlistItem();
        item.setId(req.id() != null ? req.id() : java.util.UUID.randomUUID().toString());
        item.setTicker(ticker);
        item.setName(req.name() != null ? req.name() : ticker);
        if (req.targetPrice() != null && req.targetPrice() > 0) {
            item.setTargetPrice(BigDecimal.valueOf(req.targetPrice()));
        }
        item.setNote(req.note());

        log.info("Aggiunto {} alla watchlist", ticker);
        return WatchlistItemDto.from(repository.save(item));
    }

    /**
     * Rimuove un elemento dalla watchlist per ID.
     *
     * @throws FinaiException se l'elemento non esiste (404)
     */
    @Transactional
    public void remove(String id) {
        if (!repository.existsById(id)) {
            throw new FinaiException("Elemento watchlist non trovato: " + id, 404);
        }
        repository.deleteById(id);
        log.info("Rimosso elemento watchlist {}", id);
    }
}
