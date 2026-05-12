package com.finai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.alert.AddAlertRequest;
import com.finai.dto.alert.AlertDto;
import com.finai.dto.alert.AlertsResponse;
import com.finai.exception.FinaiException;
import com.finai.service.AlertService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test funzionali per {@link AlertController}.
 */
@WebMvcTest(controllers = AlertController.class)
@DisplayName("AlertController — test HTTP")
class AlertControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private AlertService service;

    @MockBean
    private com.finai.config.RateLimitInterceptor rateLimitInterceptor;

    private static AlertDto activeAlert() {
        return new AlertDto("a-1", "AAPL", "above", 200.0, null, null, Instant.now());
    }

    @Test
    @DisplayName("GET /api/alerts → 200 con active e history")
    void getAll_returns200() throws Exception {
        when(service.getAll()).thenReturn(new AlertsResponse(
                List.of(activeAlert()), List.of()));
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", hasSize(1)))
                .andExpect(jsonPath("$.history", hasSize(0)))
                .andExpect(jsonPath("$.active[0].ticker").value("AAPL"));
    }

    @Test
    @DisplayName("POST /api/alerts → 201 con alert creato")
    void add_returns201() throws Exception {
        AddAlertRequest req = new AddAlertRequest("a-1", "AAPL", "above", 200.0);
        when(service.add(any())).thenReturn(activeAlert());
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("a-1"))
                .andExpect(jsonPath("$.type").value("above"));
    }

    @Test
    @DisplayName("POST /api/alerts → 400 se type non valido")
    void add_returns400ForInvalidType() throws Exception {
        String body = """
                {"id":"a-1","ticker":"AAPL","type":"invalid_type","value":200}
                """;
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/alerts/{id} → 200")
    void remove_returns200() throws Exception {
        doNothing().when(service).remove("a-1");
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(delete("/api/alerts/a-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("POST /api/alerts/{id}/fire → 200")
    void fire_returns200() throws Exception {
        doNothing().when(service).fire(eq("a-1"), eq(201.5));
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/alerts/a-1/fire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":201.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("POST /api/alerts/{id}/fire → 404 se alert non esiste")
    void fire_returns404IfMissing() throws Exception {
        doThrow(new FinaiException("not found", 404)).when(service).fire(eq("missing"), any());
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        mvc.perform(post("/api/alerts/missing/fire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":200.0}"))
                .andExpect(status().isNotFound());
    }
}
