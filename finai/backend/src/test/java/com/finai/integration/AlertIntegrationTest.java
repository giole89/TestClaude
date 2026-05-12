package com.finai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.alert.AddAlertRequest;
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
 * Test di integrazione per gli alert con PostgreSQL reale.
 * Copre il ciclo di vita completo: creazione → scatto → history.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Alert — integrazione con PostgreSQL")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AlertIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private com.finai.repository.AlertRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/alerts → active e history vuoti inizialmente")
    void emptyAlerts() throws Exception {
        mvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", hasSize(0)))
                .andExpect(jsonPath("$.history", hasSize(0)));
    }

    @Test
    @Order(2)
    @DisplayName("creazione alert → appare in active")
    void createAlert_appearsInActive() throws Exception {
        AddAlertRequest req = new AddAlertRequest("a-1", "AAPL", "above", 200.0);

        mvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.type").value("above"))
                .andExpect(jsonPath("$.firedAt").doesNotExist());

        mvc.perform(get("/api/alerts"))
                .andExpect(jsonPath("$.active", hasSize(1)))
                .andExpect(jsonPath("$.active[0].ticker").value("AAPL"));
    }

    @Test
    @Order(3)
    @DisplayName("fire alert → si sposta in history con prezzo e timestamp")
    void fireAlert_movesToHistory() throws Exception {
        mvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new AddAlertRequest("fire-1", "MSFT", "below", 300.0))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/alerts/fire-1/fire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":295.0}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/alerts"))
                .andExpect(jsonPath("$.active", hasSize(0)))
                .andExpect(jsonPath("$.history", hasSize(1)))
                .andExpect(jsonPath("$.history[0].firedPrice").value(295.0));
    }

    @Test
    @Order(4)
    @DisplayName("fire idempotente: secondo fire non cambia firedPrice")
    void fireTwice_idempotent() throws Exception {
        mvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new AddAlertRequest("idem-1", "NVDA", "above", 500.0))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/alerts/idem-1/fire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":505.0}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/alerts/idem-1/fire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":510.0}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/alerts"))
                .andExpect(jsonPath("$.history[0].firedPrice").value(505.0));
    }

    @Test
    @Order(5)
    @DisplayName("tutti i tipi di alert sono accettati")
    void allAlertTypes_accepted() throws Exception {
        String[] types = {"above", "below", "change_up", "change_down"};
        for (int i = 0; i < types.length; i++) {
            mvc.perform(post("/api/alerts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(
                                    new AddAlertRequest("type-" + i, "AAPL", types[i], 5.0))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value(types[i]));
        }
    }

    @Test
    @Order(6)
    @DisplayName("tipo alert non valido → 400")
    void invalidType_returns400() throws Exception {
        String body = """
                {"id":"bad-1","ticker":"AAPL","type":"wrong","value":100}
                """;
        mvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
