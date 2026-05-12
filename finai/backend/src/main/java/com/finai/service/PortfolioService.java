package com.finai.service;

import com.finai.domain.entity.PortfolioItem;
import com.finai.dto.portfolio.AddPortfolioItemRequest;
import com.finai.dto.portfolio.PortfolioItemDto;
import com.finai.dto.quote.QuoteDto;
import com.finai.exception.FinaiException;
import com.finai.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic per la gestione del portafoglio.
 *
 * <p>Le operazioni di aggiunta/rimozione sono transazionali.
 * Il refresh prezzi aggiorna in batch tutti i ticker del portafoglio
 * con i dati live di Yahoo Finance.</p>
 */
@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioRepository repository;
    private final YahooFinanceService yahooFinanceService;

    public PortfolioService(PortfolioRepository repository,
                            YahooFinanceService yahooFinanceService) {
        this.repository          = repository;
        this.yahooFinanceService = yahooFinanceService;
    }

    /** Restituisce tutte le posizioni ordinate per data di creazione discendente. */
    public List<PortfolioItemDto> getAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PortfolioItemDto::from)
                .toList();
    }

    /**
     * Aggiunge una nuova posizione al portafoglio.
     *
     * @throws FinaiException se l'ID è già presente (per idempotenza lato frontend)
     */
    @Transactional
    public PortfolioItemDto add(AddPortfolioItemRequest req) {
        if (repository.existsById(req.id())) {
            throw new FinaiException("Posizione con id=" + req.id() + " già presente", 409);
        }

        PortfolioItem item = new PortfolioItem();
        item.setId(req.id());
        item.setTicker(req.ticker().toUpperCase());
        item.setName(req.name());
        item.setQty(BigDecimal.valueOf(req.qty()));
        item.setLoadPrice(BigDecimal.valueOf(req.loadPrice()));
        item.setCurrency(req.currency() != null ? req.currency() : "USD");

        return PortfolioItemDto.from(repository.save(item));
    }

    /**
     * Rimuove una posizione dal portafoglio.
     *
     * @throws FinaiException se la posizione non esiste
     */
    @Transactional
    public void remove(String id) {
        if (!repository.existsById(id)) {
            throw new FinaiException("Posizione non trovata: " + id, 404);
        }
        repository.deleteById(id);
    }

    /**
     * Aggiorna i prezzi correnti di tutte le posizioni via Yahoo Finance.
     *
     * <p>I ticker unici vengono fetchati in un singolo batch call a Yahoo.
     * Per ogni ticker trovato aggiorna {@code currentPrice} e {@code currency}
     * per tutte le posizioni corrispondenti.</p>
     *
     * @return portafoglio aggiornato
     */
    @Transactional
    public List<PortfolioItemDto> refresh() {
        List<PortfolioItem> items = repository.findAllByOrderByCreatedAtDesc();
        if (items.isEmpty()) return List.of();

        // Raccoglie i ticker unici e li divide in chunk da 20
        Set<String> tickers = items.stream()
                .map(PortfolioItem::getTicker)
                .collect(Collectors.toSet());

        List<String> tickerList = new ArrayList<>(tickers);
        List<List<String>> chunks = partition(tickerList, 20);

        for (List<String> chunk : chunks) {
            try {
                List<QuoteDto> quotes = yahooFinanceService.fetchBatch(chunk);
                for (QuoteDto q : quotes) {
                    if (q.price() != null) {
                        repository.updatePriceByTicker(q.ticker(),
                                BigDecimal.valueOf(q.price()),
                                q.currency() != null ? q.currency() : "USD");
                    }
                    if (q.name() != null) {
                        repository.updateNameByTicker(q.ticker(), q.name());
                    }
                }
            } catch (Exception e) {
                log.warn("Errore refresh chunk {}: {}", chunk, e.getMessage());
            }
        }

        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PortfolioItemDto::from)
                .toList();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
