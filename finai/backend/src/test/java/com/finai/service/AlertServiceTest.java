package com.finai.service;

import com.finai.domain.entity.Alert;
import com.finai.domain.enums.AlertType;
import com.finai.dto.alert.AddAlertRequest;
import com.finai.dto.alert.AlertsResponse;
import com.finai.exception.FinaiException;
import com.finai.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per {@link AlertService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService")
class AlertServiceTest {

    @Mock
    private AlertRepository repository;

    @InjectMocks
    private AlertService service;

    private Alert activeAlert;

    @BeforeEach
    void setUp() {
        activeAlert = new Alert();
        activeAlert.setId("alert-1");
        activeAlert.setTicker("AAPL");
        activeAlert.setType(AlertType.ABOVE);
        activeAlert.setValue(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("getAll: separa alert attivi da history")
    void getAll_separatesActiveFromHistory() {
        Alert fired = new Alert();
        fired.setId("alert-2");
        fired.setTicker("MSFT");
        fired.setType(AlertType.BELOW);
        fired.setValue(new BigDecimal("300.00"));
        fired.setFiredAt(java.time.Instant.now());
        fired.setFiredPrice(new BigDecimal("295.0"));

        when(repository.findByFiredAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(activeAlert));
        when(repository.findByFiredAtIsNotNullOrderByFiredAtDesc()).thenReturn(List.of(fired));

        AlertsResponse response = service.getAll();

        assertThat(response.active()).hasSize(1);
        assertThat(response.active().get(0).ticker()).isEqualTo("AAPL");
        assertThat(response.history()).hasSize(1);
        assertThat(response.history().get(0).ticker()).isEqualTo("MSFT");
    }

    @Test
    @DisplayName("add: normalizza ticker in uppercase e salva")
    void add_normalizesTickerAndSaves() {
        AddAlertRequest req = new AddAlertRequest("alert-1", "aapl", "above", 200.0);
        when(repository.existsById("alert-1")).thenReturn(false);
        when(repository.save(any())).thenReturn(activeAlert);

        var dto = service.add(req);

        assertThat(dto.ticker()).isEqualTo("AAPL");
        verify(repository).save(argThat(a -> a.getType() == AlertType.ABOVE));
    }

    @Test
    @DisplayName("add: 409 se ID già presente")
    void add_throwsConflictIfDuplicate() {
        when(repository.existsById("alert-1")).thenReturn(true);

        assertThatThrownBy(() -> service.add(
                new AddAlertRequest("alert-1", "AAPL", "above", 200.0)))
                .isInstanceOf(FinaiException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("fire: imposta firedAt e firedPrice sull'alert")
    void fire_setsTimestampAndPrice() {
        when(repository.findById("alert-1")).thenReturn(Optional.of(activeAlert));
        when(repository.save(any())).thenReturn(activeAlert);

        service.fire("alert-1", 201.50);

        verify(repository).save(argThat(a ->
                a.getFiredAt() != null && a.getFiredPrice().compareTo(new BigDecimal("201.5")) == 0));
    }

    @Test
    @DisplayName("fire: idempotente se alert già scattato")
    void fire_idempotentIfAlreadyFired() {
        activeAlert.setFiredAt(java.time.Instant.now());
        activeAlert.setFiredPrice(BigDecimal.valueOf(200.0));
        when(repository.findById("alert-1")).thenReturn(Optional.of(activeAlert));

        service.fire("alert-1", 202.0);

        // Non deve salvare di nuovo
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("fire: 404 se alert non esiste")
    void fire_throwsNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fire("missing", 100.0))
                .isInstanceOf(FinaiException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("remove: 404 se alert non esiste")
    void remove_throwsNotFound() {
        when(repository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.remove("missing"))
                .isInstanceOf(FinaiException.class);
    }
}
