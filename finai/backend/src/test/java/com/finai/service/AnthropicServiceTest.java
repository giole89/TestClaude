package com.finai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitari per la logica di costruzione del system prompt in {@link AnthropicService}.
 * Il client WebClient è mockato perché la chiamata Anthropic è testata nei test di integrazione.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnthropicService — buildSystemPrompt")
class AnthropicServiceTest {

    @Mock
    private WebClient anthropicWebClient;

    private AnthropicService service;

    @BeforeEach
    void setUp() {
        service = new AnthropicService(anthropicWebClient, new ObjectMapper());
    }

    @Test
    @DisplayName("contesto null → prompt base senza sezioni contestuali")
    void nullContext_basePromptOnly() {
        String prompt = service.buildSystemPrompt(null);

        assertThat(prompt).contains("FINAI");
        assertThat(prompt).contains("italiano");
        assertThat(prompt).doesNotContain("Contesto:");
    }

    @Test
    @DisplayName("tab 'analyze' → sezione analisi tecnica inclusa")
    void analyzeTab_includesAnalysisSection() {
        Map<String, Object> ctx = Map.of(
                "tab", "analyze",
                "ticker", "AAPL",
                "tickerData", Map.of("price", 190.0, "rsi", 58));

        String prompt = service.buildSystemPrompt(ctx);

        assertThat(prompt).contains("Analisi tecnica");
        assertThat(prompt).contains("AAPL");
    }

    @Test
    @DisplayName("tab 'portfolio' → sezione portafoglio inclusa")
    void portfolioTab_includesPortfolioSection() {
        Map<String, Object> ctx = Map.of(
                "tab", "portfolio",
                "portfolioItems", java.util.List.of(),
                "totalValue", 10000.0);

        String prompt = service.buildSystemPrompt(ctx);

        assertThat(prompt).contains("Portafoglio");
    }

    @Test
    @DisplayName("tab 'compare' → sezione confronto inclusa")
    void compareTab_includesCompareSection() {
        Map<String, Object> ctx = Map.of(
                "tab", "compare",
                "tickerA", "AAPL",
                "tickerB", "MSFT");

        String prompt = service.buildSystemPrompt(ctx);

        assertThat(prompt).contains("Confronto");
        assertThat(prompt).contains("AAPL");
        assertThat(prompt).contains("MSFT");
    }

    @Test
    @DisplayName("tab sconosciuta → prompt base senza sezioni specifiche")
    void unknownTab_noSpecificSection() {
        Map<String, Object> ctx = Map.of("tab", "unknown_tab_xyz");

        String prompt = service.buildSystemPrompt(ctx);

        assertThat(prompt).contains("FINAI");
        assertThat(prompt).doesNotContain("unknown_tab_xyz");
    }

    @Test
    @DisplayName("tab 'ipo' → sezione IPO inclusa")
    void ipoTab_includesIpoSection() {
        Map<String, Object> ctx = Map.of("tab", "ipo", "ipoData", Map.of());
        String prompt = service.buildSystemPrompt(ctx);
        assertThat(prompt).contains("IPO");
    }
}
