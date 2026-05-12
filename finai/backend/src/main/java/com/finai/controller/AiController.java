package com.finai.controller;

import com.finai.dto.ai.ChatRequest;
import com.finai.service.AnthropicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Endpoint di chat AI in streaming SSE.
 *
 * <p>Usa {@link SseEmitter} per aprire una connessione SSE persistente
 * con il browser e delega lo streaming al service Anthropic.
 * Ogni richiesta viene eseguita su un thread virtuale (Java 21) per non
 * bloccare i thread Tomcat durante l'attesa dei chunk Claude.</p>
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Chat", description = "Chat advisor AI con streaming SSE")
public class AiController {

    /**
     * Pool di thread virtuali per eseguire le chiamate SSE in modo asincrono.
     * I thread virtuali (Java 21) sono leggeri e adatti per operazioni I/O-bound.
     */
    private static final ExecutorService VIRTUAL_EXEC =
            Executors.newVirtualThreadPerTaskExecutor();

    private final AnthropicService anthropicService;

    public AiController(AnthropicService anthropicService) {
        this.anthropicService = anthropicService;
    }

    /**
     * Avvia una sessione di chat con Claude in modalità streaming SSE.
     *
     * <p>Il client (browser) deve consumare lo stream con {@code EventSource}
     * o {@code fetch} in streaming. Ogni evento ha il formato:
     * {@code data: {"text":"..."}}</p>
     *
     * <p>Lo stream termina con {@code data: [DONE]}.</p>
     *
     * @param request messaggi della conversazione + contesto della tab attiva
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Chat AI con streaming SSE")
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        // Timeout generoso per le risposte AI lunghe (60 secondi)
        SseEmitter emitter = new SseEmitter(60_000L);

        VIRTUAL_EXEC.execute(() -> {
            try {
                anthropicService.streamChat(request, emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
