package com.finai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.portfolio.AddPortfolioItemRequest;
import com.finai.dto.portfolio.PortfolioItemDto;
import com.finai.exception.FinaiException;
import com.finai.service.PortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test funzionali del layer HTTP per {@link PortfolioController}.
 *
 * <p>Usa {@code @WebMvcTest} per caricare solo il controller e il layer MVC
 * (serializzazione JSON, validazione, error handling), isolando il service con Mockito.</p>
 */
@WebMvcTest(controllers = PortfolioController.class)
@DisplayName("PortfolioController — test HTTP")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private PortfolioService service;

    // Necessario per il RateLimitInterceptor nel WebMvcConfig
    @MockBean
    private com.finai.config.RateLimitInterceptor rateLimitInterceptor;

    private static PortfolioItemDto sampleDto() {
        return new PortfolioItemDto("id-1", "AAPL", "Apple Inc.",
                10.0, 175.0, 190.0, "USD", Instant.now());
    }

    @Test
    @DisplayName("GET /api/portfolio → 200 con lista")
    void getAll_returns200() throws Exception {
        when(service.getAll()).thenReturn(List.of(sampleDto()));
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].qty").value(10.0));
    }

    @Test
    @DisplayName("POST /api/portfolio → 201 con DTO")
    void add_returns201() throws Exception {
        AddPortfolioItemRequest req = new AddPortfolioItemRequest(
                "id-1", "AAPL", "Apple Inc.", 10.0, 175.0, "USD");
        when(service.add(any())).thenReturn(sampleDto());
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"))
                .andExpect(jsonPath("$.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("POST /api/portfolio → 400 se body non valido (ticker blank)")
    void add_returns400ForInvalidBody() throws Exception {
        // ticker vuoto → validazione fallisce
        String invalidBody = """
                {"id":"id-1","ticker":"","name":"Apple","qty":10,"loadPrice":175}
                """;
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/portfolio → 400 se qty negativa")
    void add_returns400ForNegativeQty() throws Exception {
        String body = """
                {"id":"id-1","ticker":"AAPL","name":"Apple","qty":-5,"loadPrice":175}
                """;
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/portfolio/{id} → 200")
    void remove_returns200() throws Exception {
        doNothing().when(service).remove("id-1");
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(delete("/api/portfolio/id-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("DELETE /api/portfolio/{id} → 404 se non esiste")
    void remove_returns404IfMissing() throws Exception {
        doThrow(new FinaiException("non trovato", 404)).when(service).remove("missing");
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(delete("/api/portfolio/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/portfolio/refresh → 200 con lista aggiornata")
    void refresh_returns200() throws Exception {
        when(service.refresh()).thenReturn(List.of(sampleDto()));
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/portfolio/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
