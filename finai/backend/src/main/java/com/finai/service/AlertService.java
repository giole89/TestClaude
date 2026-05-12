package com.finai.service;

import com.finai.domain.entity.Alert;
import com.finai.domain.enums.AlertType;
import com.finai.dto.alert.AddAlertRequest;
import com.finai.dto.alert.AlertDto;
import com.finai.dto.alert.AlertsResponse;
import com.finai.exception.FinaiException;
import com.finai.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Business logic per la gestione degli alert sui prezzi.
 *
 * <p>Un alert è <em>attivo</em> finché {@code firedAt} è null; diventa
 * <em>history</em> quando viene invocato {@link #fire}.</p>
 */
@Service
@Transactional(readOnly = true)
public class AlertService {

    private final AlertRepository repository;

    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    /** Restituisce la risposta separata in alert attivi e history. */
    public AlertsResponse getAll() {
        List<AlertDto> active  = repository.findByFiredAtIsNullOrderByCreatedAtDesc()
                .stream().map(AlertDto::from).toList();
        List<AlertDto> history = repository.findByFiredAtIsNotNullOrderByFiredAtDesc()
                .stream().map(AlertDto::from).toList();
        return new AlertsResponse(active, history);
    }

    /**
     * Aggiunge un nuovo alert.
     *
     * @throws FinaiException se l'ID è già presente
     */
    @Transactional
    public AlertDto add(AddAlertRequest req) {
        if (repository.existsById(req.id())) {
            throw new FinaiException("Alert con id=" + req.id() + " già presente", 409);
        }

        Alert alert = new Alert();
        alert.setId(req.id());
        alert.setTicker(req.ticker().toUpperCase());
        alert.setType(AlertType.fromString(req.type()));
        alert.setValue(BigDecimal.valueOf(req.value()));

        return AlertDto.from(repository.save(alert));
    }

    /**
     * Elimina un alert (attivo o in history).
     *
     * @throws FinaiException se l'alert non esiste
     */
    @Transactional
    public void remove(String id) {
        if (!repository.existsById(id)) {
            throw new FinaiException("Alert non trovato: " + id, 404);
        }
        repository.deleteById(id);
    }

    /**
     * Segna un alert come scattato al prezzo specificato.
     *
     * <p>Popola {@code firedAt} e {@code firedPrice}. Idempotente:
     * se l'alert è già scattato non modifica nulla.</p>
     *
     * @throws FinaiException se l'alert non esiste
     */
    @Transactional
    public void fire(String id, Double price) {
        Alert alert = repository.findById(id)
                .orElseThrow(() -> new FinaiException("Alert non trovato: " + id, 404));

        if (alert.getFiredAt() != null) return; // già scattato, idempotente

        alert.setFiredAt(Instant.now());
        alert.setFiredPrice(BigDecimal.valueOf(price));
        repository.save(alert);
    }
}
