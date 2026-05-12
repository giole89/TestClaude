package com.finai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.portfolio.AddPortfolioItemRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test di integrazione per il portfolio con PostgreSQL reale.
 *
 * <p>Usa il profilo {@code test} che punta al database {@code finai_test}
 * (configurato in {@code application-test.yml}). In ambienti CI con Docker
 * disponibile, sostituire con Testcontainers aggiungendo le annotazioni
 * {@code @Testcontainers}, {@code @Container} e {@code @DynamicPropertySource}.</p>
 *
 * <p>Copre l'intera stack: controller → service → repository → PostgreSQL.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Portfolio — integrazione con PostgreSQL")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PortfolioIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private com.finai.repository.PortfolioRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("portafoglio inizialmente vuoto")
    void emptyPortfolio() throws Exception {
        mvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(2)
    @DisplayName("aggiunta posizione e verifica persistenza")
    void addAndPersist() throws Exception {
        AddPortfolioItemRequest req = new AddPortfolioItemRequest(
                "id-test-1", "AAPL", "Apple Inc.", 10.0, 175.0, "USD");

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.qty").value(10.0))
                .andExpect(jsonPath("$.loadPrice").value(175.0));

        mvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("id-test-1"));
    }

    @Test
    @Order(3)
    @DisplayName("ID duplicato → 409 Conflict")
    void duplicateId_returns409() throws Exception {
        AddPortfolioItemRequest req = new AddPortfolioItemRequest(
                "dup-id", "MSFT", "Microsoft", 5.0, 300.0, "USD");

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    @DisplayName("eliminazione posizione")
    void deletePosition() throws Exception {
        AddPortfolioItemRequest req = new AddPortfolioItemRequest(
                "del-id", "NVDA", "NVIDIA", 2.0, 450.0, null);

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/portfolio/del-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        mvc.perform(get("/api/portfolio"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(5)
    @DisplayName("eliminazione id inesistente → 404")
    void deleteNonExistent_returns404() throws Exception {
        mvc.perform(delete("/api/portfolio/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Order(6)
    @DisplayName("valuta default USD se non specificata")
    void defaultCurrency() throws Exception {
        AddPortfolioItemRequest req = new AddPortfolioItemRequest(
                "cur-test", "ISP.MI", "Intesa SanPaolo", 100.0, 2.50, null);

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @Order(7)
    @DisplayName("validazione: qty negativa → 400")
    void negativeQty_returns400() throws Exception {
        String body = """
                {"id":"v-1","ticker":"AAPL","name":"Apple","qty":-1,"loadPrice":100}
                """;

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
