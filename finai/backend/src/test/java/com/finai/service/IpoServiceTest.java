package com.finai.service;

import com.finai.domain.entity.IpoWatchlistItem;
import com.finai.dto.ipo.AddIpoWatchlistRequest;
import com.finai.dto.ipo.IpoWatchlistItemDto;
import com.finai.dto.ipo.UpdateIpoWatchlistRequest;
import com.finai.dto.ipo.UpcomingIpoDto;
import com.finai.exception.FinaiException;
import com.finai.repository.IpoWatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per {@link IpoService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IpoService")
class IpoServiceTest {

    @Mock
    private IpoWatchlistRepository repository;

    @Mock
    private NasdaqService nasdaqService;

    @InjectMocks
    private IpoService service;

    private IpoWatchlistItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = new IpoWatchlistItem();
        sampleItem.setId("w-1");
        sampleItem.setCompanyName("Acme Corp");
        sampleItem.setTicker("ACME");
        sampleItem.setExpectedDate(LocalDate.of(2024, 6, 1));
        sampleItem.setLockupDays(180);
    }

    @Test
    @DisplayName("getUpcoming: delega a NasdaqService")
    void getUpcoming_delegatesToNasdaq() {
        var upcoming = List.of(new UpcomingIpoDto("1", "Acme", "ACME",
                "2024-06-01", "$10-$12", "5M", "NASDAQ", "Tech"));
        when(nasdaqService.fetchUpcoming()).thenReturn(upcoming);

        assertThat(service.getUpcoming()).hasSize(1);
        verify(nasdaqService).fetchUpcoming();
    }

    @Test
    @DisplayName("getWatchlist: mappa gli item in DTO")
    void getWatchlist_mapsToDto() {
        when(repository.findAllByOrderByExpectedDateAscCreatedAtDesc())
                .thenReturn(List.of(sampleItem));

        List<IpoWatchlistItemDto> result = service.getWatchlist();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).companyName()).isEqualTo("Acme Corp");
    }

    @Test
    @DisplayName("addToWatchlist: lockupDays default a 180 se non specificato")
    void addToWatchlist_defaultLockupDays() {
        AddIpoWatchlistRequest req = new AddIpoWatchlistRequest(
                "w-1", "ACME", "Acme Corp",
                LocalDate.of(2024, 6, 1), "NASDAQ", "Tech",
                null, null, null);

        when(repository.existsById("w-1")).thenReturn(false);
        when(repository.save(any())).thenReturn(sampleItem);

        service.addToWatchlist(req);

        verify(repository).save(argThat(i -> i.getLockupDays() == 180));
    }

    @Test
    @DisplayName("addToWatchlist: 409 se ID duplicato")
    void addToWatchlist_throwsConflict() {
        when(repository.existsById("w-1")).thenReturn(true);

        assertThatThrownBy(() -> service.addToWatchlist(
                new AddIpoWatchlistRequest("w-1", null, "Acme",
                        null, null, null, null, null, null)))
                .isInstanceOf(FinaiException.class);
    }

    @Test
    @DisplayName("updateWatchlist: aggiorna solo i campi non-null")
    void updateWatchlist_patchesOnlyNonNull() {
        when(repository.findById("w-1")).thenReturn(Optional.of(sampleItem));
        when(repository.save(any())).thenReturn(sampleItem);

        UpdateIpoWatchlistRequest patch = new UpdateIpoWatchlistRequest(
                null, null,
                LocalDate.of(2024, 6, 15), // solo ipoDate
                null, null, null, null, "Note aggiornate");

        service.updateWatchlist("w-1", patch);

        verify(repository).save(argThat(i ->
                i.getIpoDate().equals(LocalDate.of(2024, 6, 15))
                && "Note aggiornate".equals(i.getNotes())
                && "Acme Corp".equals(i.getCompanyName()))); // non cambiato
    }

    @Test
    @DisplayName("removeFromWatchlist: 404 se non esiste")
    void removeFromWatchlist_throwsNotFound() {
        when(repository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.removeFromWatchlist("missing"))
                .isInstanceOf(FinaiException.class);
    }
}
