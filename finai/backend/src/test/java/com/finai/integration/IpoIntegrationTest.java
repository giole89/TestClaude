package com.finai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.ipo.AddIpoWatchlistRequest;
import com.finai.dto.ipo.UpdateIpoWatchlistRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test di integrazione per il modulo IPO con PostgreSQL reale.
 * NasdaqService è mockato per evitare chiamate HTTP esterne nei test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("IPO — integrazione con PostgreSQL")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IpoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private com.finai.repository.IpoWatchlistRepository repository;

    @MockBean
    private com.finai.service.NasdaqService nasdaqService;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        when(nasdaqService.fetchUpcoming()).thenReturn(List.of());
        when(nasdaqService.fetchRecent()).thenReturn(List.of());
    }

    @Test
    @Order(1)
    @DisplayName("watchlist inizialmente vuota")
    void emptyWatchlist() throws Exception {
        mvc.perform(get("/api/ipo/watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(2)
    @DisplayName("aggiunta e recupero dalla watchlist")
    void addAndRetrieve() throws Exception {
        AddIpoWatchlistRequest req = new AddIpoWatchlistRequest(
                "w-1", "ACME", "Acme Corp",
                LocalDate.of(2024, 6, 1), "NASDAQ", "Technology",
                180, new BigDecimal("20.00"), "Interessante");

        mvc.perform(post("/api/ipo/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Acme Corp"))
                .andExpect(jsonPath("$.lockupDays").value(180))
                .andExpect(jsonPath("$.ipoPrice").value(20.0));

        mvc.perform(get("/api/ipo/watchlist"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("ACME"));
    }

    @Test
    @Order(3)
    @DisplayName("PATCH aggiorna solo i campi specificati")
    void patchUpdatesOnlySpecifiedFields() throws Exception {
        mvc.perform(post("/api/ipo/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new AddIpoWatchlistRequest("w-patch", null, "Beta Corp",
                                        null, null, null, null, null, null))))
                .andExpect(status().isCreated());

        UpdateIpoWatchlistRequest patch = new UpdateIpoWatchlistRequest(
                null, null, null, null, null, null, null, "Note test");

        mvc.perform(patch("/api/ipo/watchlist/w-patch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Note test"))
                .andExpect(jsonPath("$.companyName").value("Beta Corp"));
    }

    @Test
    @Order(4)
    @DisplayName("lockup remaining days calcolato dopo impostazione ipoDate")
    void lockupRemainingDays_calculated() throws Exception {
        LocalDate ipoDate = LocalDate.now().minusDays(30);
        AddIpoWatchlistRequest req = new AddIpoWatchlistRequest(
                "w-lockup", "LOCK", "Lock Corp",
                ipoDate, "NYSE", "Finance",
                180, new BigDecimal("15.00"), null);

        mvc.perform(post("/api/ipo/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        UpdateIpoWatchlistRequest patch = new UpdateIpoWatchlistRequest(
                null, null, ipoDate, null, null, null, null, null);

        mvc.perform(patch("/api/ipo/watchlist/w-lockup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockupRemainingDays").value(greaterThan(0)));
    }

    @Test
    @Order(5)
    @DisplayName("DELETE watchlist item")
    void deleteWatchlistItem() throws Exception {
        mvc.perform(post("/api/ipo/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new AddIpoWatchlistRequest("w-del", null, "Del Corp",
                                        null, null, null, null, null, null))))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/ipo/watchlist/w-del"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        mvc.perform(get("/api/ipo/watchlist"))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
