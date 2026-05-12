package com.finai.service;

import com.finai.domain.entity.IpoWatchlistItem;
import com.finai.dto.ipo.*;
import com.finai.exception.FinaiException;
import com.finai.repository.IpoWatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic per il modulo IPO:
 * <ul>
 *   <li>Calendario IPO upcoming/recenti (via {@link NasdaqService})</li>
 *   <li>CRUD della watchlist IPO personale (su PostgreSQL)</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class IpoService {

    private final IpoWatchlistRepository repository;
    private final NasdaqService nasdaqService;

    public IpoService(IpoWatchlistRepository repository, NasdaqService nasdaqService) {
        this.repository    = repository;
        this.nasdaqService = nasdaqService;
    }

    // ─────────────────────────────────── Calendario ───────────────────────────

    /** Prossime IPO dal calendario NASDAQ. Cachato in {@link NasdaqService}. */
    public List<UpcomingIpoDto> getUpcoming() {
        return nasdaqService.fetchUpcoming();
    }

    /** IPO recenti dal calendario NASDAQ del mese precedente. */
    public List<RecentIpoDto> getRecent() {
        return nasdaqService.fetchRecent();
    }

    // ─────────────────────────────────── Watchlist ────────────────────────────

    /** Restituisce tutti gli elementi della watchlist ordinati per data prevista. */
    public List<IpoWatchlistItemDto> getWatchlist() {
        return repository.findAllByOrderByExpectedDateAscCreatedAtDesc()
                .stream()
                .map(IpoWatchlistItemDto::from)
                .toList();
    }

    /**
     * Aggiunge un elemento alla watchlist.
     *
     * @throws FinaiException se l'ID è già presente
     */
    @Transactional
    public IpoWatchlistItemDto addToWatchlist(AddIpoWatchlistRequest req) {
        if (repository.existsById(req.id())) {
            throw new FinaiException("Watchlist item con id=" + req.id() + " già presente", 409);
        }

        IpoWatchlistItem item = new IpoWatchlistItem();
        item.setId(req.id());
        item.setTicker(req.ticker());
        item.setCompanyName(req.companyName());
        item.setExpectedDate(req.expectedDate());
        item.setExchange(req.exchange());
        item.setSector(req.sector());
        item.setLockupDays(req.lockupDays() != null ? req.lockupDays() : 180);
        item.setIpoPrice(req.ipoPrice());
        item.setNotes(req.notes());

        return IpoWatchlistItemDto.from(repository.save(item));
    }

    /**
     * Aggiornamento parziale (patch) di un elemento della watchlist.
     * Solo i campi non-null nella richiesta vengono aggiornati.
     *
     * @throws FinaiException se l'elemento non esiste
     */
    @Transactional
    public IpoWatchlistItemDto updateWatchlist(String id, UpdateIpoWatchlistRequest req) {
        IpoWatchlistItem item = repository.findById(id)
                .orElseThrow(() -> new FinaiException("Watchlist item non trovato: " + id, 404));

        if (req.ticker()       != null) item.setTicker(req.ticker());
        if (req.expectedDate() != null) item.setExpectedDate(req.expectedDate());
        if (req.ipoDate()      != null) item.setIpoDate(req.ipoDate());
        if (req.exchange()     != null) item.setExchange(req.exchange());
        if (req.sector()       != null) item.setSector(req.sector());
        if (req.lockupDays()   != null) item.setLockupDays(req.lockupDays());
        if (req.ipoPrice()     != null) item.setIpoPrice(req.ipoPrice());
        if (req.notes()        != null) item.setNotes(req.notes());

        return IpoWatchlistItemDto.from(repository.save(item));
    }

    /**
     * Rimuove un elemento dalla watchlist.
     *
     * @throws FinaiException se l'elemento non esiste
     */
    @Transactional
    public void removeFromWatchlist(String id) {
        if (!repository.existsById(id)) {
            throw new FinaiException("Watchlist item non trovato: " + id, 404);
        }
        repository.deleteById(id);
    }
}
