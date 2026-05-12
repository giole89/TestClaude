package com.finai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.ai.ChatMessage;
import com.finai.dto.ai.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Servizio di streaming SSE verso l'API Anthropic Claude.
 *
 * <p>Il metodo {@link #streamChat} apre un flusso SSE verso Anthropic e
 * ritrasmette i chunk di testo all'emitter del client browser in formato
 * {@code data: {"text":"..."}} compatibile con il frontend React.</p>
 *
 * <p>Il system prompt viene costruito dinamicamente in base al campo
 * {@code context.tab} della richiesta, iniettando i dati contestuali
 * (ticker, portafoglio, ecc.) così che Claude risponda in modo specializzato.</p>
 */
@Service
public class AnthropicService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicService.class);

    private final org.springframework.web.reactive.function.client.WebClient anthropicClient;
    private final ObjectMapper mapper;

    @Value("${finai.anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${finai.anthropic.model:claude-haiku-4-5-20251001}")
    private String model;

    @Value("${finai.anthropic.max-tokens:1024}")
    private int maxTokens;

    public AnthropicService(
            @Qualifier("anthropicWebClient") org.springframework.web.reactive.function.client.WebClient anthropicClient,
            ObjectMapper mapper) {
        this.anthropicClient = anthropicClient;
        this.mapper = mapper;
    }

    /**
     * Avvia una sessione di chat in streaming con Claude.
     *
     * <p>Il flusso SSE di Anthropic emette eventi di tipo {@code content_block_delta}
     * con delta di testo. Questo metodo estrae i delta e li trasmette all'emitter
     * in formato semplificato. Invia {@code [DONE]} al termine.</p>
     *
     * @param request  richiesta con messaggi e contesto
     * @param emitter  emitter SSE della risposta HTTP aperta con il browser
     */
    public void streamChat(ChatRequest request, SseEmitter emitter) {
        String systemPrompt = buildSystemPrompt(request.context());
        AtomicBoolean completed = new AtomicBoolean(false);

        // Costruisce il corpo della richiesta Anthropic
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("stream", true);
        body.put("system", systemPrompt);
        body.put("messages", request.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .collect(Collectors.toList()));

        try {
            anthropicClient.post()
                    .uri(baseUrl + "/v1/messages")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(line -> handleLine(line, emitter))
                    .doOnComplete(() -> {
                        if (!completed.getAndSet(true)) {
                            sendDone(emitter);
                        }
                    })
                    .doOnError(err -> {
                        log.error("Errore streaming Anthropic: {}", err.getMessage());
                        if (!completed.getAndSet(true)) {
                            emitter.completeWithError(err);
                        }
                    })
                    .blockLast(); // attende il completamento nel thread virtuale
        } catch (Exception e) {
            log.error("Errore avvio streaming: {}", e.getMessage());
            if (!completed.getAndSet(true)) {
                emitter.completeWithError(e);
            }
        }
    }

    // ─────────────────────────────────── SSE parsing ─────────────────────────

    private void handleLine(String line, SseEmitter emitter) {
        // Il flusso SSE di Anthropic ha il formato: "data: {...json...}"
        if (!line.startsWith("data: ")) return;
        String json = line.substring(6).trim();
        if (json.equals("[DONE]")) return;

        try {
            JsonNode event = mapper.readTree(json);
            String type = event.path("type").asText();

            // Estrae il testo dai delta di contenuto
            if ("content_block_delta".equals(type)) {
                String text = event.path("delta").path("text").asText(null);
                if (text != null && !text.isEmpty()) {
                    sendChunk(emitter, text);
                }
            }
        } catch (JsonProcessingException e) {
            // Ignora righe non-JSON (es. "event: ping")
        }
    }

    private void sendChunk(SseEmitter emitter, String text) {
        try {
            String payload = mapper.writeValueAsString(Map.of("text", text));
            emitter.send(SseEmitter.event().data(payload));
        } catch (IOException e) {
            log.debug("Emitter chiuso durante invio chunk: {}", e.getMessage());
        }
    }

    private void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    // ─────────────────────────────────── System prompt ───────────────────────

    /**
     * Costruisce il system prompt dinamico in base alla tab attiva e ai dati contestuali.
     *
     * <p>Il contesto include informazioni specifiche per tab (ticker, P&L, IPO, ecc.)
     * che permettono a Claude di rispondere in modo pertinente e accurato.</p>
     */
    String buildSystemPrompt(Map<String, Object> context) {
        String tab = context != null ? String.valueOf(context.getOrDefault("tab", "")) : "";

        StringBuilder sb = new StringBuilder("""
                Sei FINAI, un advisor finanziario AI specializzato per investitori italiani.
                Rispondi sempre in italiano, con tono professionale ma accessibile.
                Usa dati concreti quando disponibili. Non dare mai consigli di acquisto/vendita
                espliciti: fornisci analisi e lascia la decisione all'utente.
                Formatta le risposte in Markdown quando utile (elenchi, grassetti, tabelle).
                """);

        if (context == null) return sb.toString();

        switch (tab) {
            case "analyze" -> {
                sb.append("\n## Contesto: Analisi tecnica\n");
                appendJson(sb, context, "ticker");
                appendJson(sb, context, "tickerData");
            }
            case "compare" -> {
                sb.append("\n## Contesto: Confronto strumenti\n");
                appendJson(sb, context, "tickerA");
                appendJson(sb, context, "tickerB");
                appendJson(sb, context, "dataA");
                appendJson(sb, context, "dataB");
            }
            case "portfolio" -> {
                sb.append("\n## Contesto: Portafoglio personale\n");
                appendJson(sb, context, "portfolioItems");
                appendJson(sb, context, "totalValue");
                appendJson(sb, context, "totalGainPct");
            }
            case "longterm" -> {
                sb.append("\n## Contesto: Valutazione lungo termine / DCA\n");
                appendJson(sb, context, "ticker");
                appendJson(sb, context, "longTermScore");
            }
            case "ipo" -> {
                sb.append("\n## Contesto: Monitoraggio IPO\n");
                appendJson(sb, context, "ipoData");
            }
            case "market" -> {
                sb.append("\n## Contesto: Dashboard mercato\n");
                appendJson(sb, context, "indices");
                appendJson(sb, context, "sentiment");
            }
            default -> {
                // Nessun contesto specifico
            }
        }

        return sb.toString();
    }

    private void appendJson(StringBuilder sb, Map<String, Object> context, String key) {
        Object val = context.get(key);
        if (val == null) return;
        try {
            sb.append("\n**").append(key).append("**: ")
              .append(mapper.writeValueAsString(val)).append("\n");
        } catch (JsonProcessingException e) {
            sb.append("\n**").append(key).append("**: ").append(val).append("\n");
        }
    }
}
